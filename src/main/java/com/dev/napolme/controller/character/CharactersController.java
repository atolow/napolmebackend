package com.dev.napolme.controller.character;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.dto.character.CharacterResponse;
import com.dev.napolme.dto.character.CharacterSearchRequest;
import com.dev.napolme.dto.character.CharacterSearchResponse;
import com.dev.napolme.dto.character.FetchCharacterRequest;
import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.character.CharacterSummaryDto;
import com.dev.napolme.service.character.CharacterFetchService;
import com.dev.napolme.service.character.CharacterSearchService;
import com.dev.napolme.service.logging.SearchRankingService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 캐릭터 API: URL로 가져오기, 검색(server+name 또는 nickname), ID 조회, 갱신.
 * 경로: /api/characters
 */
@RestController
@RequestMapping("/api/characters")
public class CharactersController {

    private final CharacterFetchService characterFetchService;
    private final CharacterSearchService characterSearchService;
    private final SearchRankingService searchRankingService;

    public CharactersController(
        CharacterFetchService characterFetchService,
        CharacterSearchService characterSearchService,
        SearchRankingService searchRankingService
    ) {
        this.characterFetchService = characterFetchService;
        this.characterSearchService = characterSearchService;
        this.searchRankingService = searchRankingService;
    }

    /**
     * 공식 사이트 캐릭터 URL로 조회 후 저장. 이미 저장된 경우 해당 정보 반환.
     */
    @PostMapping("/fetch")
    public ResponseEntity<ApiResponse<CharacterResponse>> fetchByUrl(
        @Valid @RequestBody FetchCharacterRequest request
    ) {
        CharacterResponse response = characterFetchService.fetchByUrl(request.getUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 검색: server + name 또는 nickname 단독.
     * - server, name 있음 → 공식 API 검색 (query=name, server=server)
     * - name만 있음 (server 없음) → 전체 서버 검색 (race 파라미터 사용 가능)
     * - nickname만 있음 → DB에 저장된 캐릭터 닉네임 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> search(
        HttpServletRequest request,
        @RequestParam(required = false) String server,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String nickname,
        @RequestParam(required = false) Integer race
    ) {
        String clientIp = extractClientIp(request);
        if (nickname != null && !nickname.isBlank()) {
            List<CharacterSummaryDto> items = characterFetchService.searchByNickname(nickname);
            String redirectUrl = null;
            if (items.size() == 1) {
                CharacterSummaryDto item = items.get(0);
                redirectUrl = String.format("/character/%s/%s", 
                    item.serverId() != null ? item.serverId() : "",
                    URLEncoder.encode(item.characterId(), StandardCharsets.UTF_8));
            }
            if (!items.isEmpty()) {
                String tribe = items.get(0).tribe();
                String serverId = items.get(0).serverId() != null ? String.valueOf(items.get(0).serverId()) : null;
                searchRankingService.recordSearch(nickname, tribe, serverId, clientIp);
            }
            return ResponseEntity.ok(ApiResponse.success(new CharacterSearchResponse(
                nickname,
                null,
                items.size(),
                items,
                new CachePolicyDto(false, false),
                redirectUrl
            )));
        }
        if (name != null && !name.isBlank()) {
            CharacterSearchRequest req = new CharacterSearchRequest();
            req.setQuery(name);
            // server가 없거나 'ALL'이면 전체 검색 (서버가 자동 판단)
            if (server != null && !server.isBlank() && !server.equals("ALL")) {
                req.setServer(server);
            }
            if (race != null) {
                req.setRace(race);
            }
            CharacterSearchResponse response = characterSearchService.search(req);
            String redirectUrl = null;
            if (response.items().size() == 1) {
                CharacterSummaryDto item = response.items().get(0);
                redirectUrl = String.format("/character/%s/%s", 
                    item.serverId() != null ? item.serverId() : "",
                    URLEncoder.encode(item.characterId(), StandardCharsets.UTF_8));
            }
            if (!response.items().isEmpty()) {
                String tribe = response.items().get(0).tribe();
                String serverId = req.getServer();
                searchRankingService.recordSearch(name, tribe, serverId, clientIp);
            }
            // redirectUrl을 포함한 새로운 응답 생성
            CharacterSearchResponse responseWithRedirect = new CharacterSearchResponse(
                response.query(),
                response.server(),
                response.total(),
                response.items(),
                response.cache(),
                redirectUrl
            );
            return ResponseEntity.ok(ApiResponse.success("OK", responseWithRedirect, response.cache().cacheHit(), 0));
        }
        return ResponseEntity.badRequest().body(
            ApiResponse.failure("INVALID_PARAMS", "Either 'nickname' or 'name' is required")
        );
    }

    /**
     * serverId + characterId 로 저장 (없으면 공식 API 조회 후 저장). 상세 페이지 "정보 갱신" 시 사용.
     * Cooldown 중이면 COOLDOWN_ACTIVE 코드 반환.
     */
    @PostMapping("/fetch-by-ref")
    public ResponseEntity<ApiResponse<CharacterResponse>> fetchByRef(
        @RequestParam String serverId,
        @RequestParam String characterId
    ) {
        // Cooldown 체크
        int cooldown = characterFetchService.getRemainingRefreshCooldownSeconds(serverId, characterId);
        if (cooldown > 0) {
            return ResponseEntity.ok(ApiResponse.failure("COOLDOWN_ACTIVE", 
                String.format("%d초 후 다시 시도해주세요", cooldown), cooldown));
        }
        CharacterResponse response = characterFetchService.fetchByRef(serverId, characterId);
        int newCooldown = characterFetchService.getRemainingRefreshCooldownSeconds(serverId, characterId);
        return ResponseEntity.ok(ApiResponse.success("OK", response, false, newCooldown));
    }

    private static final int REFRESH_COOLDOWN_SECONDS = 60;

    /**
     * serverId + characterId 로 저장된 캐릭터 조회. 없으면 공식 API에서 조회 후 저장하고 반환.
     * 응답 cooldown: 남은 갱신 쿨다운(초). 새로고침 후에도 서버가 알려주므로 유지된다.
     */
    @GetMapping("/by-ref")
    public ResponseEntity<ApiResponse<CharacterResponse>> getByRef(
        @RequestParam String serverId,
        @RequestParam String characterId
    ) {
        Optional<CharacterResponse> existing = characterFetchService.getByServerIdAndCharacterId(serverId, characterId);
        if (existing.isPresent()) {
            int cooldown = characterFetchService.getRemainingRefreshCooldownSeconds(serverId, characterId);
            return ResponseEntity.ok(ApiResponse.success("OK", existing.get(), false, cooldown));
        }
        CharacterResponse response = characterFetchService.fetchByRef(serverId, characterId);
        int cooldown = characterFetchService.getRemainingRefreshCooldownSeconds(serverId, characterId);
        return ResponseEntity.ok(ApiResponse.success("OK", response, false, cooldown));
    }

    /**
     * 저장된 캐릭터 ID로 조회. 응답 cooldown: 남은 갱신 쿨다운(초).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CharacterResponse>> getById(@PathVariable Long id) {
        return characterFetchService.getById(id)
            .map(body -> {
                int cooldown = characterFetchService.getRemainingRefreshCooldownSeconds(id);
                return ResponseEntity.ok(ApiResponse.success("OK", body, false, cooldown));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 저장된 캐릭터 정보를 공식 API 기준으로 갱신. 응답 cooldown: 60(초). 이 시간 동안 재갱신 제한.
     * Cooldown 중이면 COOLDOWN_ACTIVE 코드 반환.
     */
    @PostMapping("/{id}/refresh")
    public ResponseEntity<ApiResponse<CharacterResponse>> refresh(@PathVariable Long id) {
        // Cooldown 체크
        int cooldown = characterFetchService.getRemainingRefreshCooldownSeconds(id);
        if (cooldown > 0) {
            return ResponseEntity.ok(ApiResponse.failure("COOLDOWN_ACTIVE", 
                String.format("%d초 후 다시 시도해주세요", cooldown), cooldown));
        }
        CharacterResponse response = characterFetchService.refresh(id);
        return ResponseEntity.ok(ApiResponse.success("OK", response, false, REFRESH_COOLDOWN_SECONDS));
    }

    private static String extractClientIp(HttpServletRequest request) {
        if (request == null) return "";
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
}
