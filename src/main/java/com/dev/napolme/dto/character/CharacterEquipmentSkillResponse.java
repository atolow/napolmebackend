package com.dev.napolme.dto.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.time.Instant;
import java.util.List;

public record CharacterEquipmentSkillResponse(
    List<EquipmentItem> equipmentList,
    List<EquipmentItem> skinList,
    PetInfo pet,
    WingInfo wing,
    WingInfo wingSkin,
    List<SkillItem> skillList,
    Instant lastUpdated,
    CachePolicyDto cache
) {
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

    public record PetInfo(
        Integer id,
        String name,
        Integer level,
        String icon
    ) {}

    public record WingInfo(
        Integer id,
        String name,
        Integer enchantLevel,
        String grade,
        String icon
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
