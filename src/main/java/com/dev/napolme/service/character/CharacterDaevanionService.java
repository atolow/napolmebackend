package com.dev.napolme.service.character;

import com.dev.napolme.dto.character.CharacterDaevanionBundleResponse;
import com.dev.napolme.dto.character.CharacterDaevanionDetailResponse;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterDaevanionDetailResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class CharacterDaevanionService {
    private final PlayNcClient playNcClient;
    private final Map<String, CachedDetail> detailCache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public CharacterDaevanionService(PlayNcClient playNcClient) {
        this.playNcClient = playNcClient;
    }

    public CharacterDaevanionBundleResponse fetchBundle(
        String serverId,
        String characterId,
        List<Integer> boardIds,
        String lang
    ) {
        List<CharacterDaevanionDetailResponse> boards = new ArrayList<>();
        if (boardIds == null || boardIds.isEmpty()) {
            return new CharacterDaevanionBundleResponse(boards, null);
        }
        for (Integer boardId : boardIds) {
            if (boardId == null) {
                continue;
            }
            CharacterDaevanionDetailResponse detail = fetchDetailWithRetry(
                serverId,
                characterId,
                boardId,
                lang
            );
            if (detail != null) {
                boards.add(detail);
            }
        }
        return new CharacterDaevanionBundleResponse(boards, null);
    }

    private CharacterDaevanionDetailResponse fetchDetailWithRetry(
        String serverId,
        String characterId,
        Integer boardId,
        String lang
    ) {
        String cacheKey = buildCacheKey(serverId, characterId, boardId, lang);
        CharacterDaevanionDetailResponse cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }
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
                PlayNcCharacterDaevanionDetailResponse raw =
                    playNcClient.fetchCharacterDaevanionDetail(
                        serverId,
                        characterId,
                        boardId,
                        lang
                    );
                CharacterDaevanionDetailResponse mapped = mapDetail(boardId, raw);
                putCache(cacheKey, mapped);
                return mapped;
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

    private CharacterDaevanionDetailResponse mapDetail(
        Integer boardId,
        PlayNcCharacterDaevanionDetailResponse raw
    ) {
        if (raw == null) {
            return null;
        }
        return new CharacterDaevanionDetailResponse(
            boardId,
            mapNodes(raw.nodeList()),
            mapEffects(raw.openStatEffectList()),
            mapEffects(raw.openSkillEffectList())
        );
    }

    private List<CharacterDaevanionDetailResponse.Node> mapNodes(
        List<PlayNcCharacterDaevanionDetailResponse.Node> nodes
    ) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
            .map(node -> new CharacterDaevanionDetailResponse.Node(
                node.nodeId(),
                node.name(),
                node.row(),
                node.col(),
                node.grade(),
                node.type(),
                node.icon(),
                mapEffects(node.effectList()),
                node.open()
            ))
            .collect(Collectors.toList());
    }

    private List<CharacterDaevanionDetailResponse.EffectItem> mapEffects(
        List<PlayNcCharacterDaevanionDetailResponse.EffectItem> effects
    ) {
        if (effects == null) {
            return List.of();
        }
        return effects.stream()
            .map(effect -> new CharacterDaevanionDetailResponse.EffectItem(effect.desc()))
            .collect(Collectors.toList());
    }

    private String buildCacheKey(
        String serverId,
        String characterId,
        Integer boardId,
        String lang
    ) {
        int safeBoardId = boardId == null ? 0 : boardId;
        return "server=" + serverId
            + ":char=" + characterId
            + ":board=" + safeBoardId
            + ":lang=" + lang;
    }

    private CharacterDaevanionDetailResponse getCached(String key) {
        CachedDetail cached = detailCache.get(key);
        if (cached == null) {
            return null;
        }
        if (cached.expiresAt().isBefore(Instant.now())) {
            detailCache.remove(key);
            return null;
        }
        return cached.value();
    }

    private void putCache(String key, CharacterDaevanionDetailResponse value) {
        if (value == null) {
            return;
        }
        detailCache.put(key, new CachedDetail(value, Instant.now().plus(CACHE_TTL)));
    }

    private record CachedDetail(
        CharacterDaevanionDetailResponse value,
        Instant expiresAt
    ) {}
}
