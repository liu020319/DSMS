package com.medicine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.medicine.common.Result;
import com.medicine.dto.ChangePasswordDTO;
import com.medicine.dto.RegisterDTO;
import com.medicine.dto.ResetPasswordDTO;
import com.medicine.entity.SysUser;
import com.medicine.service.SysLogService;
import com.medicine.service.SysUserService;
import com.medicine.util.AccessControl;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired private SysUserService sysUserService;
    @Autowired private SysLogService sysLogService;
    @Autowired private AccessControl accessControl;

    @GetMapping("/list")
    public Result<List<SysUser>> list(@RequestParam(required = false) String role, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        List<SysUser> users;
        if (accessControl.isSystemAdmin(request)) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            if (role != null && !role.isEmpty()) wrapper.eq(SysUser::getRole, role);
            wrapper.orderByDesc(SysUser::getCreateTime);
            users = sysUserService.list(wrapper);
        } else {
            users = sysUserService.getFamilyUsers(getUserId(request));
            if (role != null && !role.isEmpty()) users.removeIf(user -> !role.equals(user.getRole()));
        }
        users.forEach(user -> user.setPassword(null));
        return Result.success(users);
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        if (!accessControl.isSystemAdmin(request)) accessControl.requireOwnerOrAdmin(request, id);
        SysUser user = sysUserService.getById(id);
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/add")
    public Result<SysUser> add(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        if (accessControl.isSystemAdmin(request)) {
            if ("ADMIN".equals(dto.getRole())) return Result.error("平台管理员账号只能通过受控数据库迁移创建");
            if (!"GUARDIAN".equals(dto.getRole()) && !"ELDER".equals(dto.getRole())) return Result.error("角色类型不正确");
        } else {
            if (!"ELDER".equals(dto.getRole())) return Result.error("家庭守护人只能新增本家庭的安心用药成员");
            dto.setBindParentId(getUserId(request));
        }
        SysUser saved = sysUserService.register(dto);
        sysLogService.log(getUserId(request), "新增用户", "新增用户: " + dto.getUsername(), request.getRemoteAddr());
        saved.setPassword(null);
        return Result.success(saved);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysUser user, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        SysUser existing = sysUserService.getById(user.getUserId());
        if (existing == null) return Result.error("用户不存在");
        if (!accessControl.isSystemAdmin(request)) {
            accessControl.requireOwnerOrAdmin(request, user.getUserId());
            if (!getUserId(request).equals(existing.getUserId()) && !"ELDER".equals(existing.getRole())) {
                return Result.error("只能修改本人或本家庭的安心用药成员");
            }
        } else if (!"ADMIN".equals(existing.getRole())
                && !"GUARDIAN".equals(user.getRole()) && !"ELDER".equals(user.getRole())) {
            return Result.error("角色类型不正确");
        }
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        existing.setStatus(user.getStatus());
        if (accessControl.isSystemAdmin(request) && !"ADMIN".equals(existing.getRole())) {
            existing.setRole(user.getRole());
            existing.setBindParentId(user.getBindParentId());
        }
        sysUserService.updateById(existing);
        sysLogService.log(getUserId(request), "修改用户", "修改用户编号: " + user.getUserId(), request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable("id") Long id, @Valid @RequestBody ResetPasswordDTO dto,
                                      HttpServletRequest request) {
        accessControl.requireAdmin(request);
        if (!accessControl.isSystemAdmin(request)) accessControl.requireOwnerOrAdmin(request, id);
        sysUserService.resetPassword(id, dto.getNewPassword());
        sysLogService.log(getUserId(request), "重置密码", "重置用户密码，编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        sysUserService.changePassword(userId, dto.getCurrentPassword(), dto.getNewPassword());
        sysLogService.log(userId, "修改密码", "用户主动修改登录密码", request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/unlock/{id}")
    public Result<Void> unlock(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        if (!accessControl.isSystemAdmin(request)) accessControl.requireOwnerOrAdmin(request, id);
        sysUserService.unlockAccount(id);
        sysLogService.log(getUserId(request), "解锁账号", "解除账号登录锁定，用户编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/bind")
    public Result<Void> bindElder(@RequestParam Long elderId, @RequestParam Long parentId, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        SysUser elder = sysUserService.getById(elderId);
        if (elder == null || !"ELDER".equals(elder.getRole())) return Result.error("安心用药成员不存在");
        Long effectiveGuardianId = accessControl.isSystemAdmin(request) ? parentId : getUserId(request);
        if (!accessControl.isSystemAdmin(request) && elder.getBindParentId() != null
                && !effectiveGuardianId.equals(elder.getBindParentId())) {
            return Result.error("该成员已经属于其他家庭，不能直接改绑");
        }
        SysUser guardian = sysUserService.getById(effectiveGuardianId);
        if (guardian == null || (!"GUARDIAN".equals(guardian.getRole()) && !"ADMIN".equals(guardian.getRole()))) {
            return Result.error("家庭守护人或平台管理员账号不存在");
        }
        elder.setBindParentId(effectiveGuardianId);
        sysUserService.updateById(elder);
        sysLogService.log(getUserId(request), "建立家庭绑定",
                "绑定成员编号: " + elderId + " 到守护人编号: " + effectiveGuardianId, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/unbind")
    public Result<Void> unbindElder(@RequestParam Long elderId, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        SysUser elder = sysUserService.getById(elderId);
        if (elder == null || !"ELDER".equals(elder.getRole())) return Result.error("安心用药成员不存在");
        if (!accessControl.isSystemAdmin(request)) accessControl.requireOwnerOrAdmin(request, elderId);
        sysUserService.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, elderId).set(SysUser::getBindParentId, null));
        sysLogService.log(getUserId(request), "解除家庭绑定", "解除成员绑定，用户编号: " + elderId, request.getRemoteAddr());
        return Result.success();
    }

    @GetMapping("/elders/{parentId}")
    public Result<List<SysUser>> getEldersByParent(@PathVariable("parentId") Long parentId, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        if (!accessControl.isSystemAdmin(request) && !getUserId(request).equals(parentId)) {
            return Result.error(403, "无权查看其他家庭的绑定关系");
        }
        List<SysUser> elders = sysUserService.getElderByParentId(parentId);
        elders.forEach(user -> user.setPassword(null));
        return Result.success(elders);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUserOrFamily(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireSystemAdmin(request);
        SysUser target = sysUserService.getById(id);
        if (target == null) return Result.error("用户不存在");
        String description = "GUARDIAN".equals(target.getRole())
                ? "删除家庭守护人及其绑定的安心用药账号，守护人编号: " + id
                : "删除安心用药账号，编号: " + id;
        sysUserService.deleteUserOrFamily(id);
        sysLogService.log(getUserId(request), "删除账号", description, request.getRemoteAddr());
        return Result.success();
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
