package com.dev.napolme.controller.board;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.dto.board.BoardUpdateResponse;
import com.dev.napolme.dto.board.NapolmeUpdateItemDto;
import com.dev.napolme.dto.board.NapolmeUpdatesResponse;
import com.dev.napolme.service.board.BoardUpdateService;
import com.dev.napolme.service.board.NapolmeUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/board")
public class BoardUpdateController {
    private final BoardUpdateService boardUpdateService;
    private final NapolmeUpdateService napolmeUpdateService;

    public BoardUpdateController(
        BoardUpdateService boardUpdateService,
        NapolmeUpdateService napolmeUpdateService
    ) {
        this.boardUpdateService = boardUpdateService;
        this.napolmeUpdateService = napolmeUpdateService;
    }

    @GetMapping("/updates")
    public ApiResponse<BoardUpdateResponse> getUpdates(
        @RequestParam(defaultValue = "4") int size,
        @RequestParam(defaultValue = "ko") String lang
    ) {
        BoardUpdateResponse response = boardUpdateService.fetchUpdateList(size, lang);
        return ApiResponse.success("OK", response, response.cache().cacheHit(), 0);
    }

    @GetMapping("/napolme-updates")
    public ApiResponse<NapolmeUpdatesResponse> getNapolmeUpdates(HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        NapolmeUpdatesResponse response = napolmeUpdateService.getList(clientIp);
        return ApiResponse.success(response);
    }

    @PostMapping("/napolme-updates")
    public ApiResponse<NapolmeUpdateItemDto> createNapolmeUpdate(
        HttpServletRequest request,
        @RequestBody CreateNapolmeUpdateRequest body
    ) {
        String clientIp = extractClientIp(request);
        try {
            NapolmeUpdateItemDto created = napolmeUpdateService.create(
                clientIp,
                body.title(),
                body.content()
            );
            return ApiResponse.success(created);
        } catch (IllegalArgumentException e) {
            return ApiResponse.failure("FORBIDDEN", "권한이 없습니다.");
        }
    }

    private static String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String first = xForwardedFor.split(",")[0].trim();
            if (!first.isBlank()) return first;
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) return xRealIp;
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
    }

    public record CreateNapolmeUpdateRequest(String title, String content) {}
}
