package com.dev.napolme.controller.combat;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.dto.combat.CombatCompareRequest;
import com.dev.napolme.dto.combat.CombatCompareResponse;
import com.dev.napolme.dto.combat.CombatScoreRequest;
import com.dev.napolme.dto.combat.CombatScoreResponse;
import com.dev.napolme.service.combat.CombatScoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/combat-score")
public class CombatScoreController {

    private final CombatScoreService combatScoreService;

    public CombatScoreController(CombatScoreService combatScoreService) {
        this.combatScoreService = combatScoreService;
    }

    /**
     * GET: serverId + characterId로 캐릭터 정보 조회 후 나폴미 점수(전투력) 계산.
     * 캐릭터 상세 페이지에서 나폴미 점수 카드용.
     */
    @GetMapping
    public ApiResponse<CombatScoreResponse> getCombatScore(
        @RequestParam String serverId,
        @RequestParam String characterId
    ) {
        CombatScoreResponse result = combatScoreService.calculateFromCharacter(
            serverId,
            characterId,
            null,
            null
        );
        return ApiResponse.success(result);
    }

    /**
     * POST: 전투력 계산 (stats 직접 입력 또는 serverId+characterId).
     */
    @PostMapping
    public ApiResponse<CombatScoreResponse> calculateCombatScore(
        @Valid @RequestBody CombatScoreRequest request
    ) {
        CombatScoreResponse result = combatScoreService.calculate(request);
        return ApiResponse.success(result);
    }

    /**
     * POST: 여러 캐릭터 전투력 비교.
     */
    @PostMapping("/compare")
    public ApiResponse<CombatCompareResponse> compareCharacters(
        @Valid @RequestBody CombatCompareRequest request
    ) {
        CombatCompareResponse result = combatScoreService.compare(request);
        return ApiResponse.success(result);
    }
}
