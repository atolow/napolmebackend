package com.dev.napolme.service.live;

import com.dev.napolme.dto.live.ChzzkLiveItemDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 치지직 AION2 카테고리 라이브 목록 조회. 시청자 수 상위 6명 반환 (2행×3열).
 */
@Service
public class ChzzkLiveService {

    private static final String CHZZK_API =
        "https://api.chzzk.naver.com/service/v2/categories/GAME/AION2/lives";
    private static final int REQUEST_SIZE = 20;
    private static final int TOP_N = 6;
    /** 치지직 썸네일 types: 1080, 720, 480, 360, 270, 144 (small/medium 없음) */
    private static final String THUMB_TYPE = "360";

    private final RestTemplate restTemplate;

    public ChzzkLiveService(RestTemplateBuilder builder) {
        this.restTemplate = builder
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build();
    }

    /**
     * 시청자 수(concurrentUserCount) 기준 상위 6개 라이브 반환 (2행×3열).
     */
    public List<ChzzkLiveItemDto> getTop6ByViewers() {
        String url = CHZZK_API + "?size=" + REQUEST_SIZE;
        try {
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null) return List.of();

            Object content = body.get("content");
            if (!(content instanceof Map<?, ?>)) return List.of();

            Map<?, ?> contentMap = (Map<?, ?>) content;
            Object data = contentMap.get("data");
            if (!(data instanceof List<?>)) return List.of();

            List<ChzzkLiveItemDto> list = new ArrayList<>();
            for (Object raw : (List<?>) data) {
                if (!(raw instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) raw;
                ChzzkLiveItemDto dto = toDto(item);
                if (dto != null) list.add(dto);
            }

            return list.stream()
                .sorted(Comparator.comparingInt(ChzzkLiveItemDto::concurrentUserCount).reversed())
                .limit(TOP_N)
                .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String toString(Object o) {
        if (o == null) return null;
        return o.toString();
    }

    private static ChzzkLiveItemDto toDto(Map<String, Object> item) {
        try {
            Object channelObj = item.get("channel");
            if (!(channelObj instanceof Map)) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> channel = (Map<String, Object>) channelObj;
            String channelId = toString(channel.get("channelId"));
            if (channelId == null || channelId.isBlank()) return null;

            long liveId = toLong(item.get("liveId"));
            String liveTitle = toString(item.get("liveTitle"));
            String liveImageUrl = toString(item.get("liveImageUrl"));
            int concurrentUserCount = toInt(item.get("concurrentUserCount"));
            String channelName = toString(channel.get("channelName"));
            String channelImageUrl = toString(channel.get("channelImageUrl"));

            String thumbUrl = liveImageUrl == null || liveImageUrl.isBlank()
                ? null
                : liveImageUrl.replace("{type}", THUMB_TYPE);
            String liveUrl = "https://chzzk.naver.com/live/" + channelId;

            return new ChzzkLiveItemDto(
                liveId,
                liveTitle != null ? liveTitle : "",
                thumbUrl,
                concurrentUserCount,
                channelId,
                channelName != null ? channelName : "",
                channelImageUrl,
                liveUrl
            );
        } catch (Exception e) {
            return null;
        }
    }
}
