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
    public ApiResponse<NapolmeUpdatesResponse> getNapolmeUpdates(
        HttpServletRequest request,
        @RequestParam(required = false) String debug
    ) {
        String clientIp = extractClientIp(request);
        boolean includeSeenIp = "1".equals(debug);
        NapolmeUpdatesResponse response = napolmeUpdateService.getList(clientIp, includeSeenIp);
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

    /**
     * 프록시/로드밸런서 뒤에서 실제 클라이언트 IP 추출.
     * Cloudflare(CF-Connecting-IP), X-Forwarded-For(첫 값·마지막 값), X-Real-IP 순으로 확인.
     */
    private static String extractClientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) return cf.trim();

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            String first = parts[0].trim();
            if (!first.isBlank()) return first;
            if (parts.length > 1) {
                String last = parts[parts.length - 1].trim();
                if (!last.isBlank()) return last;
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) return xRealIp.trim();

        String remote = request.getRemoteAddr();
        return remote != null ? remote : "";
    }

    public record CreateNapolmeUpdateRequest(String title, String content) {}
}
