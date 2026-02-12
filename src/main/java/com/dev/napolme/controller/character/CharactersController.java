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
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 캐릭터 API: URL로 가져오기, 검색(server+name 또는 nickname), ID 조회, 갱신.
 * 경로: /api/characters
 */
@RestController
@RequestMapping("/api/characters")
public class CharactersController {

    private final CharacterFetchService characterFetchService;
    private final CharacterSearchService characterSearchService;

    public CharactersController(
        CharacterFetchService characterFetchService,
        CharacterSearchService characterSearchService
    ) {
        this.characterFetchService = characterFetchService;
        this.characterSearchService = characterSearchService;
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
     * - nickname만 있음 → DB에 저장된 캐릭터 닉네임 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> search(
        @RequestParam(required = false) String server,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String nickname
    ) {
        if (nickname != null && !nickname.isBlank()) {
            List<CharacterSummaryDto> items = characterFetchService.searchByNickname(nickname);
            return ResponseEntity.ok(ApiResponse.success(new CharacterSearchResponse(
                nickname,
                null,
                items.size(),
                items,
                new CachePolicyDto(false, false)
            )));
        }
        if (server != null && !server.isBlank() && name != null && !name.isBlank()) {
            CharacterSearchRequest req = new CharacterSearchRequest();
            req.setQuery(name);
            req.setServer(server);
            CharacterSearchResponse response = characterSearchService.search(req);
            return ResponseEntity.ok(ApiResponse.success("OK", response, response.cache().cacheHit(), 0));
        }
        return ResponseEntity.badRequest().body(
            ApiResponse.failure("INVALID_PARAMS", "Either 'nickname' or both 'server' and 'name' are required")
        );
    }

    /**
     * serverId + characterId 로 저장 (없으면 공식 API 조회 후 저장). 상세 페이지 "정보 갱신" 시 사용.
     */
    @PostMapping("/fetch-by-ref")
    public ResponseEntity<ApiResponse<CharacterResponse>> fetchByRef(
        @RequestParam String serverId,
        @RequestParam String characterId
    ) {
        CharacterResponse response = characterFetchService.fetchByRef(serverId, characterId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * serverId + characterId 로 저장된 캐릭터 조회. 없으면 공식 API에서 조회 후 저장하고 반환.
     */
    @GetMapping("/by-ref")
    public ResponseEntity<ApiResponse<CharacterResponse>> getByRef(
        @RequestParam String serverId,
        @RequestParam String characterId
    ) {
        return characterFetchService.getByServerIdAndCharacterId(serverId, characterId)
            .map(body -> ResponseEntity.ok(ApiResponse.success(body)))
            .orElseGet(() -> {
                CharacterResponse response = characterFetchService.fetchByRef(serverId, characterId);
                return ResponseEntity.ok(ApiResponse.success(response));
            });
    }

    /**
     * 저장된 캐릭터 ID로 조회.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CharacterResponse>> getById(@PathVariable Long id) {
        return characterFetchService.getById(id)
            .map(body -> ResponseEntity.ok(ApiResponse.success(body)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 저장된 캐릭터 정보를 공식 API 기준으로 갱신.
     */
    @PostMapping("/{id}/refresh")
    public ResponseEntity<ApiResponse<CharacterResponse>> refresh(@PathVariable Long id) {
        CharacterResponse response = characterFetchService.refresh(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
