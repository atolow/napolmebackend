package com.dev.napolme.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
public class AutoBlockFilter extends OncePerRequestFilter {

    private final AutoBlockService autoBlockService;

    public AutoBlockFilter(AutoBlockService autoBlockService) {
        this.autoBlockService = autoBlockService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = extractClientIp(request);
        String endpointPattern = buildEndpointPattern(request);
        String endpointSignature = sha256(endpointPattern);
        String userAgent = safeHeader(request, "User-Agent");
        String acceptLanguage = safeHeader(request, "Accept-Language");
        String acceptEncoding = safeHeader(request, "Accept-Encoding");
        String uaSignature = sha256(userAgent + "|" + acceptLanguage + "|" + acceptEncoding);

        try {
            filterChain.doFilter(request, response);
        } finally {
            autoBlockService.recordRequest(
                clientIp,
                endpointSignature,
                uaSignature,
                response.getStatus()
            );
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = safeHeader(request, "X-Forwarded-For");
        if (!xForwardedFor.isBlank()) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (!firstIp.isBlank()) {
                return firstIp;
            }
        }
        String xRealIp = safeHeader(request, "X-Real-IP");
        if (!xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String buildEndpointPattern(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return request.getMethod() + ":" + request.getRequestURI();
        }
        return request.getMethod() + ":" + request.getRequestURI() + "?" + query;
    }

    private String safeHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? "" : value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String toHex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte b : data) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
