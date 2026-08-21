package com.medicine.util;

import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAddressResolverTest {
    private final ClientAddressResolver resolver = new ClientAddressResolver();

    @Test
    void trustsForwardedAddressOnlyBehindLocalProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.18, 127.0.0.1");
        assertEquals("203.0.113.18", resolver.resolve(request));
    }

    @Test
    void ignoresSpoofedForwardedAddressFromPublicClient() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("198.51.100.7");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.99");
        assertEquals("198.51.100.7", resolver.resolve(request));
    }

    @Test
    void fallsBackToRealIpWhenForwardedChainIsInvalid() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.27");
        assertEquals("203.0.113.27", resolver.resolve(request));
    }
}
