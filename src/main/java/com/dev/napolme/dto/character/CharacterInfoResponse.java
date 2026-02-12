package com.dev.napolme.dto.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.time.Instant;
import java.util.List;

public record CharacterInfoResponse(
    String characterId,
    String name,
    Integer level,
    Integer serverId,
    String serverName,
    String regionName,
    String className,
    String raceName,
    String genderName,
    String profileImage,
    StatsSummary statsSummary,
    List<RankingItem> rankings,
    List<TitleItem> topTitles,
    List<DaevanionBoardItem> daevanionBoards,
    Instant lastUpdated,
    CachePolicyDto cache
) {
    public record StatsSummary(
        Integer itemLevel,
        List<StatItem> primaryStats,
        List<StatItem> specialStats
    ) {}

    public record StatItem(
        String type,
        String name,
        Integer value,
        List<String> statSecondList
    ) {}

    public record RankingItem(
        String contentName,
        Integer rank,
        Integer point,
        Integer prevRank,
        Integer rankChange,
        String className,
        String guildName
    ) {}

    public record TitleItem(
        Long id,
        String equipCategory,
        String name,
        String grade,
        Integer totalCount,
        Integer ownedCount,
        Integer ownedPercent,
        List<String> statList,
        List<String> equipStatList
    ) {}

    public record DaevanionBoardItem(
        Integer id,
        String name,
        Integer totalNodeCount,
        Integer openNodeCount,
        Integer openPercent,
        String icon
    ) {}
}
