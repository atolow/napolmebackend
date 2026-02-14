package com.dev.napolme.dto.board;

import java.util.List;

public record NapolmeUpdatesResponse(
    List<NapolmeUpdateItemDto> items,
    boolean allowWrite,
    String seenIp
) {}
