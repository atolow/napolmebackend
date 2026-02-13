package com.dev.napolme.controller.stat;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.domain.character.SavedCharacter;
import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.stat.NapolmeRankItemDto;
import com.dev.napolme.dto.stat.NapolmeRankingResponse;
import com.dev.napolme.dto.stat.PopularStatItemDto;
import com.dev.napolme.dto.stat.PopularStatResponse;
import com.dev.napolme.dto.live.ChzzkLiveItemDto;
import com.dev.napolme.repository.character.SavedCharacterRepository;
import com.dev.napolme.service.live.ChzzkLiveService;
import com.dev.napolme.service.logging.SearchRankingService;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stat")
public class StatController {

    private static final String TRIBE_ELYOS = "elyos";
    private static final String TRIBE_ASMO = "asmo";

    private final SearchRankingService searchRankingService;
    private final SavedCharacterRepository savedCharacterRepository;
    private final ChzzkLiveService chzzkLiveService;

    public StatController(
        SearchRankingService searchRankingService,
        SavedCharacterRepository savedCharacterRepository,
        ChzzkLiveService chzzkLiveService
    ) {
        this.searchRankingService = searchRankingService;
        this.savedCharacterRepository = savedCharacterRepository;
        this.chzzkLiveService = chzzkLiveService;
    }

    @GetMapping("/popular")
    public ApiResponse<PopularStatResponse> getPopular() {
        List<PopularStatItemDto> items = List.of();
        CachePolicyDto cache = new CachePolicyDto(false, true);
        PopularStatResponse response = new PopularStatResponse(
            "character",
            Instant.now(),
            items,
            cache
        );
        return ApiResponse.success("OK", response, cache.cacheHit(), 0);
    }

    /** 일일 검색 랭킹 TOP 10 (당일 한국 시간 기준 검색 횟수) */
    @GetMapping("/daily-search-ranking")
    public ApiResponse<List<SearchRankingService.DailySearchRankItem>> getDailySearchRanking() {
        List<SearchRankingService.DailySearchRankItem> items = searchRankingService.getDailyTop10();
        return ApiResponse.success(items);
    }

    /** 종족별 나폴미 점수 TOP 5 (천족/마족 각각) */
    @GetMapping("/napolme-ranking")
    public ApiResponse<NapolmeRankingResponse> getNapolmeRanking() {
        List<NapolmeRankItemDto> elyos = savedCharacterRepository
            .findTop5ByTribeAndNapolmePointIsNotNullOrderByNapolmePointDesc(TRIBE_ELYOS)
            .stream()
            .map(StatController::toRankItem)
            .collect(Collectors.toList());
        List<NapolmeRankItemDto> asmo = savedCharacterRepository
            .findTop5ByTribeAndNapolmePointIsNotNullOrderByNapolmePointDesc(TRIBE_ASMO)
            .stream()
            .map(StatController::toRankItem)
            .collect(Collectors.toList());
        return ApiResponse.success(new NapolmeRankingResponse(elyos, asmo));
    }

    /** 치지직 아이온2 라이브 시청자 수 상위 6 (2행×3열, 30분마다 갱신 권장) */
    @GetMapping("/chzzk-lives")
    public ApiResponse<List<ChzzkLiveItemDto>> getChzzkLives() {
        try {
            List<ChzzkLiveItemDto> items = chzzkLiveService.getTop6ByViewers();
            return ApiResponse.success(items != null ? items : List.of());
        } catch (Exception e) {
            return ApiResponse.success(List.of());
        }
    }

    private static NapolmeRankItemDto toRankItem(SavedCharacter c) {
        return new NapolmeRankItemDto(
            c.getNickname(),
            c.getNapolmePoint(),
            c.getServerId(),
            c.getServerName()
        );
    }
}
