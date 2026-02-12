package com.dev.napolme.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AutoBlockService {

    private static final String BLOCK_KEY_PREFIX = "block:ip:";
    private static final String FAILURE_KEY_PREFIX = "ab:fail:";
    private static final String PATTERN_KEY_PREFIX = "ab:pattern:";
    private static final String UA_SET_PREFIX = "ab:ua:";

    private final StringRedisTemplate redisTemplate;
    private final AutoBlockProperties properties;

    public AutoBlockService(StringRedisTemplate redisTemplate, AutoBlockProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void recordRequest(String clientIp, String endpointSignature, String uaSignature, int status) {
        if (!properties.isEnabled()) {
            return;
        }
        if (isBlocked(clientIp)) {
            return;
        }

        if (shouldCountFailure(status) && recordFailure(clientIp)) {
            return;
        }
        if (recordPattern(clientIp, endpointSignature)) {
            return;
        }
        recordUaSignature(clientIp, uaSignature);
    }

    private boolean shouldCountFailure(int status) {
        if (status < 400) {
            return false;
        }
        return status != 403 && status != 429;
    }

    private boolean recordFailure(String clientIp) {
        String key = FAILURE_KEY_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofSeconds(properties.getFailureWindowSeconds()));
        if (count != null && count >= properties.getFailureThreshold()) {
            blockIp(clientIp);
            return true;
        }
        return false;
    }

    private boolean recordPattern(String clientIp, String endpointSignature) {
        String key = PATTERN_KEY_PREFIX + clientIp + ":" + endpointSignature;
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofSeconds(properties.getPatternWindowSeconds()));
        if (count != null && count >= properties.getPatternThreshold()) {
            blockIp(clientIp);
            return true;
        }
        return false;
    }

    private boolean recordUaSignature(String clientIp, String uaSignature) {
        String key = UA_SET_PREFIX + clientIp;
        redisTemplate.opsForSet().add(key, uaSignature);
        redisTemplate.expire(key, Duration.ofSeconds(properties.getUaWindowSeconds()));
        Long size = redisTemplate.opsForSet().size(key);
        if (size != null && size > properties.getUaUniqueThreshold()) {
            blockIp(clientIp);
            return true;
        }
        return false;
    }

    private boolean isBlocked(String clientIp) {
        String key = BLOCK_KEY_PREFIX + clientIp;
        Boolean blocked = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(blocked);
    }

    private void blockIp(String clientIp) {
        String key = BLOCK_KEY_PREFIX + clientIp;
        redisTemplate.opsForValue().set(
            key,
            "1",
            Duration.ofSeconds(properties.getBlockSeconds())
        );
    }
}
