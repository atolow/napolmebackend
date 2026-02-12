package com.dev.napolme.rateLimit;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final RateLimitProperties properties;
    private final RedisRateLimiter redisRateLimiter;

    public RateLimitService(RateLimitProperties properties, RedisRateLimiter redisRateLimiter) {
        this.properties = properties;
        this.redisRateLimiter = redisRateLimiter;
    }

    public boolean isAllowed(String clientIp, String endpointKey) {
        if (!properties.isEnabled()) {
            return true;
        }
        String ipKey = "rate:ip:" + clientIp;
        boolean ipAllowed = redisRateLimiter.tryConsume(
            ipKey,
            properties.getPerSecond(),
            properties.getBurst(),
            1
        );
        if (!ipAllowed) {
            return false;
        }

        int weight = resolveEndpointWeight(endpointKey);
        String endpointRateKey = "rate:ip:endpoint:" + clientIp + ":" + endpointKey;
        return redisRateLimiter.tryConsume(
            endpointRateKey,
            properties.getPerSecond(),
            properties.getBurst(),
            weight
        );
    }

    public int getCooldownSeconds() {
        return properties.getCooldownSeconds();
    }

    private int resolveEndpointWeight(String endpointKey) {
        Map<String, Integer> weights = properties.getEndpointWeights();
        if (weights == null || weights.isEmpty()) {
            return properties.getDefaultWeight();
        }
        Integer weight = weights.get(endpointKey);
        if (weight != null) {
            return weight;
        }
        return properties.getDefaultWeight();
    }
}
