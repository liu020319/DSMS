package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.entity.SysUser;
import com.medicine.service.SysLogService;
import com.medicine.service.SysUserService;
import com.medicine.util.AccessControl;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public Result<SysUser> add(@RequestBody SysUser user, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        user.setPassword(null);
        SysUser saved = sysUserService.register(
                new com.medicine.dto.RegisterDTO() {{
                    setUsername(user.getUsername());
                    setPassword("123456");
                    setRealName(user.getRealName());
                    setPhone(user.getPhone());
                    setRole(user.getRole());
                    setBindParentId(user.getBindParentId());
                }});
        sysLogService.log(getUserId(request), "新增用户", "新增用户: " + user.getUsername(), request.getRemoteAddr());
        saved.setPassword(null);
        return Result.success(saved);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysUser user, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        user.setPassword(null);
        sysUserService.updateById(user);
        sysLogService.log(getUserId(request), "修改用户", "修改用户，编号: " + user.getUserId(), request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable("id") Long id,
                                       @RequestParam(defaultValue = "123456") String newPassword,
                                       HttpServletRequest request) {
        accessControl.requireAdmin(request);
        sysUserService.resetPassword(id, newPassword);
        sysLogService.log(getUserId(request), "重置密码", "重置密码，用户编号: " + id, request.getRemoteAddr());
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
