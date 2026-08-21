package com.medicine.util;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 只在请求确实来自本机/内网反向代理时读取转发头，避免公网客户端伪造来源地址。
 */
@Component
public class ClientAddressResolver {
    public String resolve(HttpServletRequest request) {
        String remoteAddress = clean(request.getRemoteAddr());
        if (!isTrustedProxy(remoteAddress)) return remoteAddress;

        String forwarded = firstValidForwardedAddress(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) return forwarded;
        String realAddress = clean(request.getHeader("X-Real-IP"));
        return isIpLiteral(realAddress) ? realAddress : remoteAddress;
    }

    private String firstValidForwardedAddress(String value) {
        if (value == null) return null;
        for (String part : value.split(",")) {
            String candidate = clean(part);
            if (isIpLiteral(candidate)) return candidate;
        }
        return null;
    }

    private boolean isTrustedProxy(String value) {
        if (value == null) return false;
        if ("::1".equals(value) || "0:0:0:0:0:0:0:1".equals(value)) return true;
        if (value.startsWith("127.") || value.startsWith("10.") || value.startsWith("192.168.")) return true;
        if (value.startsWith("172.")) {
            String[] parts = value.split("\\.");
            if (parts.length == 4) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        String lower = value.toLowerCase();
        return lower.startsWith("fc") || lower.startsWith("fd") || lower.startsWith("fe80:");
    }

    private boolean isIpLiteral(String value) {
        if (value == null || value.length() > 64 || "unknown".equalsIgnoreCase(value)) return false;
        if (value.indexOf(':') >= 0) return value.matches("[0-9A-Fa-f:.%]+") && !value.contains("..");
        String[] parts = value.split("\\.");
        if (parts.length != 4) return false;
        for (String part : parts) {
            try {
                if (part.isEmpty() || Integer.parseInt(part) < 0 || Integer.parseInt(part) > 255) return false;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }

    private String clean(String value) {
        if (value == null) return "unknown";
        String cleaned = value.trim();
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }
}
