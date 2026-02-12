package com.dev.napolme.dto.plaync.character;

import java.util.List;

public record PlayNcCharacterEquipmentSkillResponse(
    Equipment equipment,
    Petwing petwing,
    Skill skill
) {
    public record Equipment(
        List<EquipmentItem> equipmentList,
        List<EquipmentItem> skinList
    ) {}

    public record EquipmentItem(
        Long id,
        String name,
        Integer enchantLevel,
        Integer exceedLevel,
        String grade,
        Integer slotPos,
        String slotPosName,
        String icon
    ) {}

    public record Petwing(
        Pet pet,
        Wing wing,
        Wing wingSkin
    ) {}

    public record Pet(
        Integer id,
        String name,
        Integer level,
        String icon
    ) {}

    public record Wing(
        Integer id,
        String name,
        Integer enchantLevel,
        String grade,
        String icon
    ) {}

    public record Skill(
        List<SkillItem> skillList
    ) {}

    public record SkillItem(
        Integer id,
        String name,
        Integer needLevel,
        Integer skillLevel,
        String icon,
        String category,
        Integer acquired,
        Integer equip
    ) {}
}
