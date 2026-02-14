package com.dev.napolme.controller.board;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.dto.board.BoardUpdateResponse;
import com.dev.napolme.dto.board.NapolmeUpdateItemDto;
import com.dev.napolme.dto.board.NapolmeUpdatesResponse;
import com.dev.napolme.service.board.BoardUpdateService;
import com.dev.napolme.service.board.NapolmeUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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

    /**
     * 응답이 클라이언트 IP에 따라 달라지므로 캐시하면 안 됨.
     * (allowWrite는 1.236.123.32일 때만 true → 캐시 시 다른 사용자에게 잘못된 값 전달)
     */
    @GetMapping("/napolme-updates")
    public ResponseEntity<ApiResponse<NapolmeUpdatesResponse>> getNapolmeUpdates(HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        NapolmeUpdatesResponse response = napolmeUpdateService.getList(clientIp);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore().mustRevalidate())
            .body(ApiResponse.success(response));
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
