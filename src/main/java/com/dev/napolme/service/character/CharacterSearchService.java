package com.dev.napolme.service.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.character.CharacterSearchRequest;
import com.dev.napolme.dto.character.CharacterSearchResponse;
import com.dev.napolme.dto.character.CharacterSummaryDto;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterInfoResponse;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterSearchResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CharacterSearchService {
    private static final int DEFAULT_SIZE = 40;
    private static final long COMBAT_CACHE_TTL_MS = Duration.ofMinutes(10).toMillis();
    private static final int[] COMBAT_RETRY_DELAYS_MS = new int[] { 0, 300, 600 };

    private final PlayNcClient playNcClient;
    private final Map<String, CacheEntry> combatCache = new ConcurrentHashMap<>();

    public CharacterSearchService(PlayNcClient playNcClient) {
        this.playNcClient = playNcClient;
    }

    public CharacterSearchResponse search(CharacterSearchRequest request) {
        String keyword = request.getQuery();
        Integer race = request.getRace();
        String serverId = request.getServer();
        int size = request.getLimit() == null ? DEFAULT_SIZE : request.getLimit();

        PlayNcCharacterSearchResponse raw = playNcClient.fetchCharacterSearch(
            keyword,
            race,
            serverId,
            1,
            size
        );

        CachePolicyDto cache = new CachePolicyDto(false, true);
        if (raw == null || raw.list() == null) {
            return new CharacterSearchResponse(keyword, serverId, 0, List.of(), cache);
        }

        String normalizedKeyword = normalizeKeyword(keyword);

        List<CharacterSummaryDto> items = new ArrayList<>();
        for (PlayNcCharacterSearchResponse.SearchItem item : raw.list()) {
            if (item == null) {
                continue;
            }
            String name = stripHtml(item.name());
            if (!isExactMatch(name, normalizedKeyword)) {
                continue;
            }
            Integer combatPower = fetchCombatPowerWithRetry(
                item.serverId(),
                decodeCharacterId(item.characterId())
            );
            String tribe = item.race() != null && item.race() == 1 ? "elyos" : item.race() != null && item.race() == 2 ? "asmo" : null;
            items.add(new CharacterSummaryDto(
                decodeCharacterId(item.characterId()),
                name,
                item.level(),
                item.serverId(),
                item.serverName(),
                item.race(),
                item.pcId(),
                normalizeProfileUrl(item.profileImageUrl()),
                combatPower,
                tribe
            ));
        }

        items.sort((a, b) -> compareCombatPowerDesc(a.combatPower(), b.combatPower()));

        int total = raw.pagination() == null || raw.pagination().total() == null
            ? items.size()
            : raw.pagination().total();

        return new CharacterSearchResponse(keyword, serverId, total, items, cache);
    }

    private Integer fetchCombatPowerWithRetry(Integer serverId, String characterId) {
        if (serverId == null || characterId == null || characterId.isBlank()) {
            return null;
        }
        String cacheKey = buildCombatCacheKey(serverId, characterId);
        Integer cached = getCachedCombatPower(cacheKey);
        if (cached != null) {
            return cached < 0 ? null : cached;
        }
        for (int delay : COMBAT_RETRY_DELAYS_MS) {
            if (delay > 0) {
                sleep(delay);
            }
            try {
                PlayNcCharacterInfoResponse info = playNcClient.fetchCharacterInfo(
                    String.valueOf(serverId),
                    characterId,
                    "ko"
                );
                Integer itemLevel = extractItemLevel(info);
                putCombatPowerCache(cacheKey, itemLevel);
                return itemLevel;
            } catch (Exception ex) {
                if (delay == COMBAT_RETRY_DELAYS_MS[COMBAT_RETRY_DELAYS_MS.length - 1]) {
                    putCombatPowerCache(cacheKey, null);
                    return null;
                }
            }
        }
        return null;
    }

    private Integer extractItemLevel(PlayNcCharacterInfoResponse info) {
        if (info == null || info.stat() == null || info.stat().statList() == null) {
            return null;
        }
        for (PlayNcCharacterInfoResponse.StatItem item : info.stat().statList()) {
            if (item == null) {
                continue;
            }
            if ("ItemLevel".equals(item.type())) {
                return item.value();
            }
        }
        return null;
    }

    private String buildCombatCacheKey(Integer serverId, String characterId) {
        return serverId + ":" + characterId;
    }

    private Integer getCachedCombatPower(String cacheKey) {
        CacheEntry entry = combatCache.get(cacheKey);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() >= entry.expiresAt) {
            combatCache.remove(cacheKey);
            return null;
        }
        return entry.value;
    }

    private void putCombatPowerCache(String cacheKey, Integer combatPower) {
        int value = combatPower == null ? -1 : combatPower;
        combatCache.put(cacheKey, new CacheEntry(value, System.currentTimeMillis() + COMBAT_CACHE_TTL_MS));
    }

    private void sleep(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String stripHtml(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("<[^>]*>", "");
    }

    private String normalizeProfileUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("http")) {
            return value;
        }
        return "https://profileimg.plaync.com" + value;
    }

    private String decodeCharacterId(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("%3D", "=");
    }

    private String normalizeKeyword(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private boolean isExactMatch(String name, String normalizedKeyword) {
        if (normalizedKeyword.isBlank()) {
            return false;
        }
        return normalizedKeyword.equals(name);
    }

    private int compareCombatPowerDesc(Integer left, Integer right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return Integer.compare(right, left);
    }

    private static final class CacheEntry {
        private final int value;
        private final long expiresAt;

        private CacheEntry(int value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}
