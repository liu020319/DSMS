package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.dto.ChangePasswordDTO;
import com.medicine.dto.RegisterDTO;
import com.medicine.dto.ResetPasswordDTO;
import com.medicine.entity.SysUser;
import com.medicine.service.SysLogService;
import com.medicine.service.SysUserService;
import com.medicine.util.AccessControl;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private AccessControl accessControl;

    @GetMapping("/list")
    public Result<List<SysUser>> list(@RequestParam(required = false) String role, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, role);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        List<SysUser> users = sysUserService.list(wrapper);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PostMapping("/add")
    public Result<SysUser> add(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
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
        if (!"ADMIN".equals(user.getRole()) && !"ELDER".equals(user.getRole())) return Result.error("角色类型不正确");
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        existing.setRole(user.getRole());
        existing.setStatus(user.getStatus());
        existing.setBindParentId(user.getBindParentId());
        sysUserService.updateById(existing);
        sysLogService.log(getUserId(request), "修改用户", "修改用户，编号: " + user.getUserId(), request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable("id") Long id,
                                       @Valid @RequestBody ResetPasswordDTO dto,
                                       HttpServletRequest request) {
        accessControl.requireAdmin(request);
        sysUserService.resetPassword(id, dto.getNewPassword());
        sysLogService.log(getUserId(request), "重置密码", "重置密码，用户编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto,
                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        sysUserService.changePassword(userId, dto.getCurrentPassword(), dto.getNewPassword());
        sysLogService.log(userId, "修改密码", "用户主动修改登录密码", request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/unlock/{id}")
    public Result<Void> unlock(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        sysUserService.unlockAccount(id);
        sysLogService.log(getUserId(request), "解锁账号", "解除账号登录锁定，用户编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/bind")
    public Result<Void> bindElder(@RequestParam Long elderId, @RequestParam Long parentId, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        SysUser elder = sysUserService.getById(elderId);
        if (elder == null) {
            return Result.error("老人用户不存在");
        }
        elder.setBindParentId(parentId);
        sysUserService.updateById(elder);
        sysLogService.log(getUserId(request), "绑定老人", "绑定老人编号: " + elderId + " 到子女编号: " + parentId, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/unbind")
    public Result<Void> unbindElder(@RequestParam Long elderId, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        SysUser elder = sysUserService.getById(elderId);
        if (elder == null || !"ELDER".equals(elder.getRole())) return Result.error("安心用药端账号不存在");
        elder.setBindParentId(null);
        sysUserService.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, elderId).set(SysUser::getBindParentId, null));
        sysLogService.log(getUserId(request), "解除家庭绑定", "解除安心用药端账号绑定，用户编号: " + elderId, request.getRemoteAddr());
        return Result.success();
    }

    @GetMapping("/elders/{parentId}")
    public Result<List<SysUser>> getEldersByParent(@PathVariable("parentId") Long parentId, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        List<SysUser> elders = sysUserService.getElderByParentId(parentId);
        elders.forEach(u -> u.setPassword(null));
        return Result.success(elders);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
