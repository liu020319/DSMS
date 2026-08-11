package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    com.medicine.vo.LoginVO login(com.medicine.dto.LoginDTO dto);
    SysUser register(com.medicine.dto.RegisterDTO dto);
    SysUser getByUsername(String username);
    void resetPassword(Long userId, String newPassword);
    void unlockAccount(Long userId);
    void changePassword(Long userId, String currentPassword, String newPassword);
    java.util.List<SysUser> getElderByParentId(Long parentId);
    java.util.List<SysUser> getFamilyUsers(Long guardianId);
    void deleteUserOrFamily(Long userId);
}
