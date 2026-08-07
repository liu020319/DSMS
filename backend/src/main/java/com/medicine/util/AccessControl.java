package com.medicine.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.common.BusinessException;
import com.medicine.entity.SysUser;
import com.medicine.mapper.SysUserMapper;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 服务端家庭数据访问边界。ADMIN 是唯一平台管理员；GUARDIAN 是普通家庭守护人；
 * ELDER 是安心用药成员。前端隐藏菜单不能替代这里的服务端校验。
 */
@Component
public class AccessControl {

    @Autowired
    private SysUserMapper sysUserMapper;

    public void requireAdmin(HttpServletRequest request) {
        String role = role(request);
        if (!"ADMIN".equals(role) && !"GUARDIAN".equals(role)) {
            throw new BusinessException(403, "无权执行此操作");
        }
    }

    public void requireSystemAdmin(HttpServletRequest request) {
        if (!isSystemAdmin(request)) {
            throw new BusinessException(403, "此操作仅限平台管理员");
        }
    }

    public void requireElder(HttpServletRequest request) {
        if (!"ELDER".equals(role(request))) {
            throw new BusinessException(403, "此操作仅限安心用药成员");
        }
    }

    public boolean isSystemAdmin(HttpServletRequest request) {
        return "ADMIN".equals(role(request));
    }

    public boolean isGuardian(HttpServletRequest request) {
        return "GUARDIAN".equals(role(request));
    }

    public void requireOwnerOrAdmin(HttpServletRequest request, Long targetUserId) {
        if (targetUserId == null) {
            throw new BusinessException(403, "缺少需要访问的家庭成员");
        }
        if (isSystemAdmin(request)) return;
        Long currentUserId = userId(request);
        if (targetUserId.equals(currentUserId)) return;
        if (isGuardian(request) && isManagedElder(currentUserId, targetUserId)) return;
        throw new BusinessException(403, "无权访问其他家庭的数据");
    }

    /**
     * 返回当前后台账号允许查询的成员 ID。null 表示平台管理员可查看全平台；
     * 空列表表示家庭守护人尚未绑定成员，查询必须返回空结果。
     */
    public List<Long> scopedUserIds(HttpServletRequest request, Long requestedUserId) {
        requireAdmin(request);
        if (isSystemAdmin(request)) {
            return requestedUserId == null ? null : Collections.singletonList(requestedUserId);
        }
        List<Long> managed = managedElderIds(userId(request));
        if (requestedUserId != null) {
            if (!managed.contains(requestedUserId)) {
                throw new BusinessException(403, "无权访问其他家庭的数据");
            }
            return Collections.singletonList(requestedUserId);
        }
        return managed;
    }

    public List<Long> managedElderIds(Long guardianId) {
        if (guardianId == null) return Collections.emptyList();
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getBindParentId, guardianId)
                .eq(SysUser::getRole, "ELDER")
                .eq(SysUser::getDeleted, 0));
        List<Long> result = new ArrayList<>();
        for (SysUser user : users) result.add(user.getUserId());
        return result;
    }

    public boolean isManagedElder(Long guardianId, Long elderId) {
        if (guardianId == null || elderId == null) return false;
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserId, elderId)
                .eq(SysUser::getBindParentId, guardianId)
                .eq(SysUser::getRole, "ELDER")
                .eq(SysUser::getDeleted, 0)) > 0;
    }

    public Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String role(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role == null ? "" : String.valueOf(role);
    }
}
