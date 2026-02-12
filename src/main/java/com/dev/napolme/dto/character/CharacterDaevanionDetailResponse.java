package com.dev.napolme.dto.character;

import java.util.List;

public record CharacterDaevanionDetailResponse(
    Integer boardId,
    List<Node> nodeList,
    List<EffectItem> openStatEffectList,
    List<EffectItem> openSkillEffectList
) {
    public record Node(
        Integer nodeId,
        String name,
        Integer row,
        Integer col,
        String grade,
        String type,
        String icon,
        List<EffectItem> effectList,
        Integer open
    ) {}

    public record EffectItem(
        String desc
    ) {}
}
