package com.dev.napolme.dto.plaync.character;

import java.util.List;

public record PlayNcCharacterSearchResponse(
    List<SearchItem> list,
    Pagination pagination
) {
    public record SearchItem(
        String characterId,
        String name,
        Integer race,
        Integer pcId,
        Integer level,
        Integer serverId,
        String serverName,
        String profileImageUrl
    ) {}

    public record Pagination(
        Integer page,
        Integer size,
        Integer total,
        Integer endPage
    ) {}
}
