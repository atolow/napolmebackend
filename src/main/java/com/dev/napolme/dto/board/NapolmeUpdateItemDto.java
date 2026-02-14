package com.dev.napolme.dto.board;

import java.time.Instant;

public record NapolmeUpdateItemDto(
    Long id,
    String title,
    String content,
    Instant createdAt
) {}
