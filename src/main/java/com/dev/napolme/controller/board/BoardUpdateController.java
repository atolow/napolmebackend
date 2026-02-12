package com.dev.napolme.controller.board;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.dto.board.BoardUpdateResponse;
import com.dev.napolme.service.board.BoardUpdateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/board")
public class BoardUpdateController {
    private final BoardUpdateService boardUpdateService;

    public BoardUpdateController(BoardUpdateService boardUpdateService) {
        this.boardUpdateService = boardUpdateService;
    }

    @GetMapping("/updates")
    public ApiResponse<BoardUpdateResponse> getUpdates(
        @RequestParam(defaultValue = "4") int size,
        @RequestParam(defaultValue = "ko") String lang
    ) {
        BoardUpdateResponse response = boardUpdateService.fetchUpdateList(size, lang);
        return ApiResponse.success("OK", response, response.cache().cacheHit(), 0);
    }
}
