package com.dev.napolme.dto.stat;

/**
 * 나폴미 점수 랭킹 한 건 (닉네임, 점수, 서버ID, 서버명).
 */
public record NapolmeRankItemDto(
    String nickname,
    Integer napolmePoint,
    String serverId,
    String serverName
) {}
