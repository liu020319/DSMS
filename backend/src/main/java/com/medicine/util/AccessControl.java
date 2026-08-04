package com.medicine.util;

import com.medicine.common.BusinessException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 服务端访问控制，不能只依赖前端路由隐藏菜单。
 */
@Component
public class AccessControl {

    public void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException(403, "无权执行此操作");
        }
    }

    public void requireElder(HttpServletRequest request) {
        if (!"ELDER".equals(request.getAttribute("role"))) {
            throw new BusinessException(403, "此操作仅限老人账号");
        }
    }

    public void requireOwnerOrAdmin(HttpServletRequest request, Long targetUserId) {
        if ("ADMIN".equals(request.getAttribute("role"))) {
            return;
        }
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null || !currentUserId.equals(targetUserId)) {
            throw new BusinessException(403, "无权访问其他用户的数据");
        }
    }
}
