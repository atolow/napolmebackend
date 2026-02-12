package com.dev.napolme.service.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.character.CharacterEquipmentDetailBundleResponse;
import com.dev.napolme.dto.character.CharacterEquipmentItemResponse;
import com.dev.napolme.dto.character.CharacterEquipmentSkillResponse;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterEquipmentItemResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Service;

@Service
public class CharacterEquipmentItemService {
    private final PlayNcClient playNcClient;
    private final Map<String, CachedItem> detailCache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public CharacterEquipmentItemService(PlayNcClient playNcClient) {
        this.playNcClient = playNcClient;
    }

    public CharacterEquipmentItemResponse fetchItemDetail(
        Long id,
        Integer enchantLevel,
        String characterId,
        String serverId,
        Integer slotPos,
        String lang
    ) {
        String cacheKey = buildCacheKey(id, enchantLevel, characterId, serverId, slotPos, lang);
        CharacterEquipmentItemResponse cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }
        PlayNcCharacterEquipmentItemResponse raw = playNcClient.fetchCharacterEquipmentItem(
            id,
            enchantLevel,
            characterId,
            serverId,
            slotPos,
            lang
        );

        CachePolicyDto cache = new CachePolicyDto(false, true);
        if (raw == null) {
            CharacterEquipmentItemResponse response = new CharacterEquipmentItemResponse(
                id,
                null,
                null,
                null,
                null,
                null,
                null,
                enchantLevel,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                cache
            );
            putCache(cacheKey, response);
            return response;
        }

        CharacterEquipmentItemResponse response = new CharacterEquipmentItemResponse(
            raw.id(),
            raw.name(),
            raw.grade(),
            raw.gradeName(),
            raw.icon(),
            raw.level(),
            raw.levelValue(),
            raw.enchantLevel(),
            raw.maxEnchantLevel(),
            raw.maxExceedEnchantLevel(),
            raw.raceName(),
            raw.classNames(),
            raw.categoryName(),
            raw.equipLevel(),
            raw.magicStoneSlotCount(),
            raw.godStoneSlotCount(),
            raw.costumes(),
            raw.subStatCount(),
            raw.subSkillCountMax(),
            raw.subStatRandom(),
            mapMainStats(raw.mainStats()),
            raw.soulBindRate(),
            mapSubStats(raw.subStats()),
            mapSubSkills(raw.subSkills()),
            mapMagicStoneStats(raw.magicStoneStat()),
            mapGodStoneStats(raw.godStoneStat()),
            raw.sources(),
            cache
        );
        putCache(cacheKey, response);
        return response;
    }

    public List<CharacterEquipmentDetailBundleResponse.ItemDetail> fetchItemDetailsBundle(
        List<CharacterEquipmentSkillResponse.EquipmentItem> items,
        String characterId,
        String serverId,
        String lang
    ) {
        List<CharacterEquipmentDetailBundleResponse.ItemDetail> details = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return details;
        }
        for (CharacterEquipmentSkillResponse.EquipmentItem item : items) {
            if (item == null || item.id() == null) {
                continue;
            }
            Integer totalEnchant = (item.enchantLevel() == null ? 0 : item.enchantLevel())
                + (item.exceedLevel() == null ? 0 : item.exceedLevel());
            CharacterEquipmentItemResponse detail = fetchItemDetailWithRetry(
                item.id(),
                totalEnchant,
                characterId,
                serverId,
                item.slotPos(),
                lang
            );
            String key = buildItemKey(item.id(), item.slotPos(), totalEnchant);
            details.add(new CharacterEquipmentDetailBundleResponse.ItemDetail(
                key,
                item.id(),
                item.slotPos(),
                item.slotPosName(),
                item.enchantLevel(),
                item.exceedLevel(),
                detail
            ));
        }
        return details;
    }

    private CharacterEquipmentItemResponse fetchItemDetailWithRetry(
        Long id,
        Integer enchantLevel,
        String characterId,
        String serverId,
        Integer slotPos,
        String lang
    ) {
        int[] delays = new int[] { 0, 300, 600 };
        for (int attempt = 0; attempt < delays.length; attempt++) {
            if (delays[attempt] > 0) {
                try {
                    Thread.sleep(delays[attempt]);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                return fetchItemDetail(id, enchantLevel, characterId, serverId, slotPos, lang);
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (status == 429 || status >= 500) {
                    continue;
                }
                return null;
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private String buildItemKey(Long id, Integer slotPos, Integer enchantLevel) {
        long safeId = id == null ? 0L : id;
        int safeSlot = slotPos == null ? 0 : slotPos;
        int safeEnchant = enchantLevel == null ? 0 : enchantLevel;
        return safeId + "-" + safeSlot + "-" + safeEnchant;
    }

    private String buildCacheKey(
        Long id,
        Integer enchantLevel,
        String characterId,
        String serverId,
        Integer slotPos,
        String lang
    ) {
        long safeId = id == null ? 0L : id;
        int safeSlot = slotPos == null ? 0 : slotPos;
        int safeEnchant = enchantLevel == null ? 0 : enchantLevel;
        return "id=" + safeId
            + ":enchant=" + safeEnchant
            + ":char=" + characterId
            + ":server=" + serverId
            + ":slot=" + safeSlot
            + ":lang=" + lang;
    }

    private CharacterEquipmentItemResponse getCached(String key) {
        CachedItem cached = detailCache.get(key);
        if (cached == null) {
            return null;
        }
        if (cached.expiresAt().isBefore(Instant.now())) {
            detailCache.remove(key);
            return null;
        }
        return cached.value();
    }

    private void putCache(String key, CharacterEquipmentItemResponse value) {
        detailCache.put(key, new CachedItem(value, Instant.now().plus(CACHE_TTL)));
    }

    private record CachedItem(CharacterEquipmentItemResponse value, Instant expiresAt) {}

    private List<CharacterEquipmentItemResponse.MainStat> mapMainStats(
        List<PlayNcCharacterEquipmentItemResponse.MainStat> stats
    ) {
        if (stats == null) {
            return List.of();
        }
        return stats.stream()
            .map(stat -> new CharacterEquipmentItemResponse.MainStat(
                stat.name(),
                stat.minValue(),
                stat.value(),
                stat.extra(),
                stat.exceed()
            ))
            .collect(Collectors.toList());
    }

    private List<CharacterEquipmentItemResponse.SubStat> mapSubStats(
        List<PlayNcCharacterEquipmentItemResponse.SubStat> stats
    ) {
        if (stats == null) {
            return List.of();
        }
        return stats.stream()
            .map(stat -> new CharacterEquipmentItemResponse.SubStat(
                stat.name(),
                stat.value()
            ))
            .collect(Collectors.toList());
    }

    private List<CharacterEquipmentItemResponse.SubSkill> mapSubSkills(
        List<PlayNcCharacterEquipmentItemResponse.SubSkill> skills
    ) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream()
            .map(skill -> new CharacterEquipmentItemResponse.SubSkill(
                skill.id(),
                skill.level(),
                skill.icon(),
                skill.name()
            ))
            .collect(Collectors.toList());
    }

    private List<CharacterEquipmentItemResponse.MagicStoneStat> mapMagicStoneStats(
        List<PlayNcCharacterEquipmentItemResponse.MagicStoneStat> stats
    ) {
        if (stats == null) {
            return List.of();
        }
        return stats.stream()
            .map(stat -> new CharacterEquipmentItemResponse.MagicStoneStat(
                stat.icon(),
                stat.value(),
                stat.name(),
                stat.grade(),
                stat.slotPos()
            ))
            .collect(Collectors.toList());
    }

    private List<CharacterEquipmentItemResponse.GodStoneStat> mapGodStoneStats(
        List<PlayNcCharacterEquipmentItemResponse.GodStoneStat> stats
    ) {
        if (stats == null) {
            return List.of();
        }
        return stats.stream()
            .map(stat -> new CharacterEquipmentItemResponse.GodStoneStat(
                stat.icon(),
                stat.name(),
                stat.desc(),
                stat.grade(),
                stat.slotPos()
            ))
            .collect(Collectors.toList());
    }
}
