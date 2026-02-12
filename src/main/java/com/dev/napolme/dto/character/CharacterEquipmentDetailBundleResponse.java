package com.dev.napolme.dto.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.util.List;

public record CharacterEquipmentDetailBundleResponse(
    CharacterEquipmentSkillResponse equipment,
    List<ItemDetail> details,
    CachePolicyDto cache
) {
    public record ItemDetail(
        String key,
        Long id,
        Integer slotPos,
        String slotPosName,
        Integer enchantLevel,
        Integer exceedLevel,
        CharacterEquipmentItemResponse detail
    ) {}
}
