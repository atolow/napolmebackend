package com.dev.napolme.common.response;

import java.time.Instant;

public class ApiResponse<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final boolean cacheHit;
    private final int cooldown;
    private final Instant timestamp;

    private ApiResponse(
        boolean success,
        String code,
        String message,
        T data,
        boolean cacheHit,
        int cooldown,
        Instant timestamp
    ) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.cacheHit = cacheHit;
        this.cooldown = cooldown;
        this.timestamp = timestamp;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "OK", data, false, 0, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data, false, 0, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data, boolean cacheHit, int cooldown) {
        return new ApiResponse<>(true, "OK", message, data, cacheHit, cooldown, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null, false, 0, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String code, String message, int cooldown) {
        return new ApiResponse<>(false, code, message, null, false, cooldown, Instant.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public int getCooldown() {
        return cooldown;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
