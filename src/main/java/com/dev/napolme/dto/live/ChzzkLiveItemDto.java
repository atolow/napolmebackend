package com.dev.napolme.dto.live;

/**
 * 치지직 라이브 한 건 (시청자 순 상위 5용).
 */
public record ChzzkLiveItemDto(
    long liveId,
    String liveTitle,
    String liveImageUrl,
    int concurrentUserCount,
    String channelId,
    String channelName,
    String channelImageUrl,
    String liveUrl
) {}
