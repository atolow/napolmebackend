package com.dev.napolme.logging;

import com.dev.napolme.domain.logging.RequestLog;
import com.dev.napolme.service.logging.RequestLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String ANON_COOKIE_NAME = "napolme_anon_id";
    private static final Duration ANON_COOKIE_TTL = Duration.ofDays(365);
    private static final List<String> HEADER_ALLOWLIST = List.of(
        "x-forwarded-for",
        "x-real-ip",
        "user-agent",
        "referer",
        "origin",
        "accept",
        "accept-language",
        "accept-encoding",
        "content-type"
    );

    private final RequestLogService requestLogService;

    public RequestLoggingFilter(RequestLogService requestLogService) {
        this.requestLogService = requestLogService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long startNs = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        String clientIp = extractClientIp(request);
        String userAgent = safeHeader(request, "User-Agent");
        String anonId = ensureAnonId(request, response);
        String actorKey = fingerprint(clientIp, userAgent, anonId);
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String headerSummary = summarizeHeaders(request);
        Instant timestamp = Instant.now();

        log.info(
            "request id={} ip={} ua={} anonId={} actorKey={} endpoint={} headers={} ts={}",
            requestId,
            clientIp,
            userAgent,
            anonId,
            actorKey,
            endpoint,
            headerSummary,
            timestamp
        );

        boolean success = false;
        try {
            filterChain.doFilter(request, response);
            success = response.getStatus() < 400;
        } catch (Exception ex) {
            success = false;
            throw ex;
        } finally {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNs).toMillis();
            RequestLog requestLog = new RequestLog(
                requestId,
                clientIp,
                userAgent,
                anonId,
                endpoint,
                success,
                durationMs
            );
            requestLogService.saveAsync(requestLog);
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

    private String safeHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? "" : value;
    }

    private String ensureAnonId(HttpServletRequest request, HttpServletResponse response) {
        String existing = readCookie(request, ANON_COOKIE_NAME);
        if (!existing.isBlank()) {
            return existing;
        }
        String newId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(ANON_COOKIE_NAME, newId)
            .httpOnly(true)
            .secure(request.isSecure())
            .path("/")
            .maxAge(ANON_COOKIE_TTL)
            .sameSite("Lax")
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return newId;
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null ? "" : value;
            }
        }
        return "";
    }

    private String fingerprint(String ip, String ua, String anonId) {
        String payload = ip + "|" + ua + "|" + anonId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
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

    private String summarizeHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (isAllowedHeader(headerName)) {
                String value = request.getHeader(headerName);
                pairs.add(headerName.toLowerCase() + "=" + value);
            }
        }
        return String.join("|", pairs);
    }

    private boolean isAllowedHeader(String headerName) {
        for (String allowed : HEADER_ALLOWLIST) {
            if (allowed.equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }
}
