package com.dev.napolme.dto.plaync.character;

import java.util.List;

public record PlayNcCharacterInfoResponse(
    Stat stat,
    Daevanion daevanion,
    Ranking ranking,
    Profile profile,
    Title title
) {
    public record Stat(List<StatItem> statList) {}

    public record StatItem(
        String type,
        String name,
        Integer value,
        List<String> statSecondList
    ) {}

    public record Daevanion(List<DaevanionBoard> boardList) {}

    public record DaevanionBoard(
        Integer id,
        String name,
        Integer totalNodeCount,
        Integer openNodeCount,
        String icon,
        Integer open,
        Integer openPercent
    ) {}

    public record Ranking(List<RankingItem> rankingList) {}

    public record RankingItem(
        Integer rankingContentsType,
        String rankingContentsName,
        Integer rankingType,
        Integer rank,
        String characterId,
        String characterName,
        Integer classId,
        String className,
        String guildName,
        Integer point,
        Integer prevRank,
        Integer rankChange,
        Integer gradeId,
        String gradeName,
        String gradeIcon,
        String profileImage,
        Object extraDataMap
    ) {}

    public record Profile(
        String characterId,
        String characterName,
        Integer serverId,
        String serverName,
        String regionName,
        Integer pcId,
        String className,
        Integer raceId,
        String raceName,
        Integer gender,
        String genderName,
        Integer characterLevel,
        Integer titleId,
        String titleName,
        String titleGrade,
        String profileImage
    ) {}

    public record Title(
        Integer totalCount,
        Integer ownedCount,
        List<TitleItem> titleList
    ) {}

    public record TitleItem(
        Long id,
        String equipCategory,
        String name,
        String grade,
        Integer totalCount,
        Integer ownedCount,
        Integer ownedPercent,
        List<StatDesc> statList,
        List<StatDesc> equipStatList
    ) {}

    public record StatDesc(String desc) {}
}
