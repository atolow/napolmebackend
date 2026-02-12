package com.dev.napolme.dto.plaync.board;

import java.util.List;

public record PlayNcBoardUpdateResponse(
    List<ContentItem> contentList
) {
    public record ContentItem(
        String id,
        String title,
        Timestamps timestamps,
        RootBoard rootBoard
    ) {}

    public record Timestamps(
        String postDateTime
    ) {}

    public record RootBoard(
        Board board
    ) {}

    public record Board(
        String boardUrlPattern
    ) {}
}
