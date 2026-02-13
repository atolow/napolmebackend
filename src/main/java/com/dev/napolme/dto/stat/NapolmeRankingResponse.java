package com.dev.napolme.dto.stat;

import java.util.List;

/**
 * 종족별 나폴미 점수 TOP 5 응답.
 */
public record NapolmeRankingResponse(
    List<NapolmeRankItemDto> elyos,
    List<NapolmeRankItemDto> asmo
) {}
