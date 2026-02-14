package com.dev.napolme.repository.logging;

/**
 * SearchLogRepository findDailyTop10 JPQL 결과 매핑용.
 * (name, serverId, tribe, cnt) 순서로 생성자 인자.
 */
public record SearchLogRankRow(String name, String serverId, String tribe, Long cnt) {}
