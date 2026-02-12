package com.dev.napolme.dto.plaync.character;

import java.util.List;

public record PlayNcCharacterEquipmentItemResponse(
    Long id,
    String name,
    String grade,
    String gradeName,
    String icon,
    Integer level,
    Integer levelValue,
    Integer enchantLevel,
    Boolean storable,
    Boolean tradable,
    Boolean tradablePersonal,
    Boolean enchantable,
    Boolean decomposable,
    Integer maxEnchantLevel,
    Integer maxExceedEnchantLevel,
    String raceName,
    List<String> classNames,
    String categoryName,
    String type,
    Integer equipLevel,
    Integer magicStoneSlotCount,
    Integer godStoneSlotCount,
    List<String> costumes,
    Integer subStatCount,
    Integer subSkillCountMax,
    Boolean subStatRandom,
    List<MainStat> mainStats,
    String soulBindRate,
    List<SubStat> subStats,
    List<SubSkill> subSkills,
    List<MagicStoneStat> magicStoneStat,
    List<GodStoneStat> godStoneStat,
    List<String> sources
) {
    public record MainStat(
        String id,
        String name,
        String minValue,
        String value,
        String extra,
        Boolean exceed
    ) {}

    public record SubStat(
        String id,
        String name,
        String value
    ) {}

    public record SubSkill(
        Integer id,
        Integer level,
        String icon,
        String name
    ) {}

    public record MagicStoneStat(
        String id,
        String icon,
        String value,
        String name,
        String grade,
        Integer slotPos
    ) {}

    public record GodStoneStat(
        String icon,
        String name,
        String desc,
        String grade,
        Integer slotPos
    ) {}
}
