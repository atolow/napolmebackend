package com.dev.napolme.service.board;

import com.dev.napolme.dto.board.BoardUpdateItemDto;
import com.dev.napolme.dto.board.BoardUpdateResponse;
import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.plaync.board.PlayNcBoardUpdateResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class BoardUpdateService {
    private static final int[] RETRY_DELAYS_MS = new int[] { 0, 300, 600 };
    private static final long CACHE_TTL_MS = Duration.ofMinutes(30).toMillis();
    private static final String BASE_URL = "https://aion2.plaync.com";

    private final PlayNcClient playNcClient;
    private final Object cacheLock = new Object();
    private volatile CacheEntry cacheEntry;

    public BoardUpdateService(PlayNcClient playNcClient) {
        this.playNcClient = playNcClient;
    }

    public BoardUpdateResponse fetchUpdateList(int size, String lang) {
        CacheEntry cached = getCache();
        if (cached != null) {
            return new BoardUpdateResponse(
                cached.items,
                cached.lastUpdated,
                new CachePolicyDto(true, true)
            );
        }

        synchronized (cacheLock) {
            cached = getCache();
            if (cached != null) {
                return new BoardUpdateResponse(
                    cached.items,
                    cached.lastUpdated,
                    new CachePolicyDto(true, true)
                );
            }
            List<BoardUpdateItemDto> items = fetchWithRetry(size, lang);
            Instant now = Instant.now();
            CacheEntry entry = new CacheEntry(items, now, System.currentTimeMillis() + CACHE_TTL_MS);
            cacheEntry = entry;
            return new BoardUpdateResponse(items, now, new CachePolicyDto(false, true));
        }
    }

    private List<BoardUpdateItemDto> fetchWithRetry(int size, String lang) {
        List<BoardUpdateItemDto> fallback = getStaleItems();
        for (int delay : RETRY_DELAYS_MS) {
            if (delay > 0) {
                sleep(delay);
            }
            try {
                PlayNcBoardUpdateResponse root = playNcClient.fetchBoardUpdateArticles(size);
                List<BoardUpdateItemDto> items = extractItems(root);
                if (!items.isEmpty()) {
                    return items;
                }
            } catch (Exception ex) {
                if (delay == RETRY_DELAYS_MS[RETRY_DELAYS_MS.length - 1] && fallback != null) {
                    return fallback;
                }
            }
        }
        return fallback == null ? List.of() : fallback;
    }

    private List<BoardUpdateItemDto> extractItems(PlayNcBoardUpdateResponse root) {
        if (root == null || root.contentList() == null) {
            return List.of();
        }
        List<BoardUpdateItemDto> items = new ArrayList<>();
        for (PlayNcBoardUpdateResponse.ContentItem item : root.contentList()) {
            if (item == null) {
                continue;
            }
            String articleId = item.id();
            String title = item.title();
            String date = item.timestamps() == null ? null : item.timestamps().postDateTime();
            String urlPattern = item.rootBoard() == null || item.rootBoard().board() == null
                ? null
                : item.rootBoard().board().boardUrlPattern();
            String url = buildArticleUrl(urlPattern, articleId);
            if (title == null || url == null) {
                continue;
            }
            items.add(new BoardUpdateItemDto(title, url, date));
        }
        return items;
    }

    private String buildArticleUrl(String urlPattern, String articleId) {
        if (articleId == null || articleId.isBlank()) {
            return null;
        }
        if (urlPattern != null && urlPattern.contains("{articleId}")) {
            return urlPattern.replace("{articleId}", articleId);
        }
        return BASE_URL + "/ko-kr/board/update/view?articleId=" + articleId;
    }

    private CacheEntry getCache() {
        CacheEntry cached = cacheEntry;
        if (cached == null) {
            return null;
        }
        if (System.currentTimeMillis() >= cached.expiresAt) {
            return null;
        }
        return cached;
    }

    private List<BoardUpdateItemDto> getStaleItems() {
        CacheEntry cached = cacheEntry;
        if (cached == null) {
            return null;
        }
        return cached.items;
    }

    private void sleep(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class CacheEntry {
        private final List<BoardUpdateItemDto> items;
        private final Instant lastUpdated;
        private final long expiresAt;

        private CacheEntry(List<BoardUpdateItemDto> items, Instant lastUpdated, long expiresAt) {
            this.items = List.copyOf(Objects.requireNonNullElse(items, List.of()));
            this.lastUpdated = lastUpdated;
            this.expiresAt = expiresAt;
        }
    }
}
