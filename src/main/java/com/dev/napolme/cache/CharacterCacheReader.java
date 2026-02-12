package com.dev.napolme.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CharacterCacheReader {
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CharacterLockService lockService;

    public CharacterCacheReader(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        CharacterLockService lockService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.lockService = lockService;
    }

    public String readProfile(String serverId, String characterName) {
        String key = CacheKeys.characterProfile(serverId, characterName);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        String token = lockService.tryLock(serverId, characterName, LOCK_TTL);
        if (token == null) {
            return dummyProfile(serverId, characterName, "busy");
        }
        try {
            // TODO: place for future crawling + cache write
            return dummyProfile(serverId, characterName, "acquired");
        } finally {
            lockService.unlock(serverId, characterName, token);
        }
    }

    public String readStats(String serverId, String characterName) {
        String key = CacheKeys.characterStats(serverId, characterName);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        String token = lockService.tryLock(serverId, characterName, LOCK_TTL);
        if (token == null) {
            return dummyStats(serverId, characterName, "busy");
        }
        try {
            // TODO: place for future crawling + cache write
            return dummyStats(serverId, characterName, "acquired");
        } finally {
            lockService.unlock(serverId, characterName, token);
        }
    }

    private String dummyProfile(String serverId, String characterName, String lockStatus) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("serverId", serverId);
        data.put("characterName", characterName);
        data.put("source", "dummy");
        data.put("lockStatus", lockStatus);
        data.put("level", 0);
        data.put("job", "unknown");
        return toJson(data);
    }

    private String dummyStats(String serverId, String characterName, String lockStatus) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("serverId", serverId);
        data.put("characterName", characterName);
        data.put("source", "dummy");
        data.put("lockStatus", lockStatus);
        data.put("power", 0);
        data.put("hp", 0);
        data.put("mp", 0);
        return toJson(data);
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize dummy cache data", e);
        }
    }
}
