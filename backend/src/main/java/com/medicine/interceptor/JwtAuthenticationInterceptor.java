package com.medicine.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicine.common.Result;
import com.medicine.util.JwtUtil;
import com.medicine.entity.SysUser;
import com.medicine.mapper.SysUserMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            writeErrorResponse(response, 401, "未登录或登录已过期");
            return false;
        }
        token = token.substring(7);
        try {
            if (jwtUtil.isTokenExpired(token)) {
                writeErrorResponse(response, 401, "登录已过期，请重新登录");
                return false;
            }
            Long userId = jwtUtil.getUserId(token);
            SysUser currentUser = sysUserMapper.selectById(userId);
            if (currentUser == null || !Integer.valueOf(1).equals(currentUser.getStatus())) {
                writeErrorResponse(response, 401, "账号不存在或已停用，请重新登录");
                return false;
            }
            request.setAttribute("userId", currentUser.getUserId());
            request.setAttribute("username", currentUser.getUsername());
            // 每次请求读取数据库中的实时角色，避免角色调整后旧 JWT 继续保留越权权限。
            request.setAttribute("role", currentUser.getRole());
            return true;
        } catch (Exception e) {
            writeErrorResponse(response, 401, "无效的Token");
            return false;
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        Result<?> result = Result.error(code, message);
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(result));
    }
}
