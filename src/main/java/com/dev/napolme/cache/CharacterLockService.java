package com.dev.napolme.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class CharacterLockService {
    private static final RedisScript<Long> UNLOCK_SCRIPT = buildUnlockScript();

    private final StringRedisTemplate redisTemplate;

    public CharacterLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String tryLock(String serverId, String characterName, Duration ttl) {
        String key = CacheKeys.characterLock(serverId, characterName);
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public void unlock(String serverId, String characterName, String token) {
        if (token == null) {
            return;
        }
        String key = CacheKeys.characterLock(serverId, characterName);
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
    }

    private static RedisScript<Long> buildUnlockScript() {
        String script = "if redis.call('GET', KEYS[1]) == ARGV[1] then "
            + "return redis.call('DEL', KEYS[1]) "
            + "else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
