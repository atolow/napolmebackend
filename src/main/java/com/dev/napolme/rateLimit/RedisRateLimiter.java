package com.dev.napolme.rateLimit;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter {

    private static final RedisScript<Long> TOKEN_BUCKET_SCRIPT = buildScript();
    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryConsume(String key, long perSecond, long burst, int cost) {
        long nowMillis = System.currentTimeMillis();
        long ttlSeconds = calculateTtlSeconds(perSecond, burst);
        long safeCost = Math.max(1, cost);
        long safeRate = Math.max(1, perSecond);
        long safeBurst = Math.max(1, burst);

        Long result = redisTemplate.execute(
            TOKEN_BUCKET_SCRIPT,
            List.of(key),
            String.valueOf(nowMillis),
            String.valueOf(safeRate),
            String.valueOf(safeBurst),
            String.valueOf(safeCost),
            String.valueOf(ttlSeconds)
        );
        return result != null && result == 1L;
    }

    private static long calculateTtlSeconds(long perSecond, long burst) {
        long safeRate = Math.max(1, perSecond);
        long safeBurst = Math.max(1, burst);
        long baseSeconds = (long) Math.ceil((double) safeBurst / (double) safeRate);
        return Math.max(1, baseSeconds * 2);
    }

    private static RedisScript<Long> buildScript() {
        String script = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local rate = tonumber(ARGV[2])
            local burst = tonumber(ARGV[3])
            local cost = tonumber(ARGV[4])
            local ttl = tonumber(ARGV[5])

            local data = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(data[1])
            local ts = tonumber(data[2])

            if tokens == nil then
                tokens = burst
            end
            if ts == nil then
                ts = now
            end

            local delta = math.max(0, now - ts)
            local filled = math.min(burst, tokens + (delta * rate / 1000.0))
            local allowed = 0

            if filled >= cost then
                tokens = filled - cost
                allowed = 1
            else
                tokens = filled
            end

            redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
            redis.call('EXPIRE', key, ttl)
            return allowed
            """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
