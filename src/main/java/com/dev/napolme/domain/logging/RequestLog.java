package com.dev.napolme.domain.logging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "request_logs")
public class RequestLog {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(nullable = false, length = 512)
    private String ua;

    @Column(nullable = false, length = 36)
    private String cookieId;

    @Column(nullable = false, length = 512)
    private String endpoint;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false, name = "response_time_ms")
    private long responseTimeMs;

    @Column(nullable = false, name = "created_at", updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    protected RequestLog() {}

    public RequestLog(
        String requestId,
        String ip,
        String ua,
        String cookieId,
        String endpoint,
        boolean success,
        long responseTimeMs
    ) {
        this.requestId = requestId;
        this.ip = ip;
        this.ua = ua;
        this.cookieId = cookieId;
        this.endpoint = endpoint;
        this.success = success;
        this.responseTimeMs = responseTimeMs;
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now(SEOUL);
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIp() {
        return ip;
    }

    public String getUa() {
        return ua;
    }

    public String getCookieId() {
        return cookieId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
