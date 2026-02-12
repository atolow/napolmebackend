package com.dev.napolme.controller.character;

import com.dev.napolme.common.response.ApiResponse;
import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.character.CharacterDaevanionBundleResponse;
import com.dev.napolme.dto.character.CharacterDetailResponse;
import com.dev.napolme.dto.character.CharacterEquipmentDetailBundleResponse;
import com.dev.napolme.dto.character.CharacterEquipmentSkillResponse;
import com.dev.napolme.dto.character.CharacterEquipmentItemResponse;
import com.dev.napolme.dto.character.CharacterInfoResponse;
import com.dev.napolme.dto.character.CharacterSearchRequest;
import com.dev.napolme.dto.character.CharacterSearchResponse;
import com.dev.napolme.service.character.CharacterSearchService;
import com.dev.napolme.service.character.CharacterDaevanionService;
import com.dev.napolme.service.character.CharacterEquipmentService;
import com.dev.napolme.service.character.CharacterEquipmentItemService;
import com.dev.napolme.service.character.CharacterAnalysisService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/character")
public class CharacterController {
    private final CharacterAnalysisService characterAnalysisService;
    private final CharacterEquipmentService characterEquipmentService;
    private final CharacterSearchService characterSearchService;
    private final CharacterEquipmentItemService characterEquipmentItemService;
    private final CharacterDaevanionService characterDaevanionService;

    public CharacterController(
        CharacterAnalysisService characterAnalysisService,
        CharacterEquipmentService characterEquipmentService,
        CharacterSearchService characterSearchService,
        CharacterEquipmentItemService characterEquipmentItemService,
        CharacterDaevanionService characterDaevanionService
    ) {
        this.characterAnalysisService = characterAnalysisService;
        this.characterEquipmentService = characterEquipmentService;
        this.characterSearchService = characterSearchService;
        this.characterEquipmentItemService = characterEquipmentItemService;
        this.characterDaevanionService = characterDaevanionService;
    }

    @GetMapping("/search")
    public ApiResponse<CharacterSearchResponse> search(@ModelAttribute CharacterSearchRequest request) {
        CharacterSearchResponse response = characterSearchService.search(request);
        return ApiResponse.success("OK", response, response.cache().cacheHit(), 0);
    }

    @GetMapping("/{server}/{name}")
    public ApiResponse<CharacterDetailResponse> getCharacter(
        @PathVariable String server,
        @PathVariable String name
    ) {
        CachePolicyDto cache = new CachePolicyDto(false, true);
        CharacterDetailResponse response = new CharacterDetailResponse(
            server,
            name,
            null,
            null,
            null,
            null,
            null,
            Instant.now(),
            cache
        );
        return ApiResponse.success("OK", response, cache.cacheHit(), 0);
    }

    @GetMapping("/info")
    public ApiResponse<CharacterInfoResponse> getCharacterInfo(
        @RequestParam String serverId,
        @RequestParam String characterId,
        @RequestParam(defaultValue = "ko") String lang
    ) {
        CharacterInfoResponse response = characterAnalysisService.fetchCharacterInfo(
            serverId,
            characterId,
            lang
        );
        return ApiResponse.success("OK", response, response.cache().cacheHit(), 0);
    }

    @GetMapping("/equipment")
    public ApiResponse<CharacterEquipmentSkillResponse> getCharacterEquipment(
        @RequestParam String serverId,
        @RequestParam String characterId,
        @RequestParam(defaultValue = "ko") String lang
    ) {
        CharacterEquipmentSkillResponse response =
            characterEquipmentService.fetchEquipmentAndSkills(serverId, characterId, lang);
        return ApiResponse.success("OK", response, response.cache().cacheHit(), 0);
    }

    @GetMapping("/equipment/bundle")
    public ApiResponse<CharacterEquipmentDetailBundleResponse> getCharacterEquipmentBundle(
        @RequestParam String serverId,
        @RequestParam String characterId,
        @RequestParam(defaultValue = "ko") String lang
    ) {
        CharacterEquipmentSkillResponse equipment =
            characterEquipmentService.fetchEquipmentAndSkills(serverId, characterId, lang);
        var details = characterEquipmentItemService.fetchItemDetailsBundle(
            equipment.equipmentList(),
            characterId,
            serverId,
            lang
        );
        CachePolicyDto cache = new CachePolicyDto(false, true);
        CharacterEquipmentDetailBundleResponse response =
            new CharacterEquipmentDetailBundleResponse(equipment, details, cache);
        return ApiResponse.success("OK", response, cache.cacheHit(), 0);
    }

    @GetMapping("/daevanion/bundle")
    public ApiResponse<CharacterDaevanionBundleResponse> getCharacterDaevanionBundle(
        @RequestParam String serverId,
        @RequestParam String characterId,
        @RequestParam String boardIds,
        @RequestParam(defaultValue = "ko") String lang
    ) {
        List<Integer> parsedBoardIds = Arrays.stream(boardIds.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(Integer::valueOf)
            .collect(Collectors.toList());
        CharacterDaevanionBundleResponse response =
            characterDaevanionService.fetchBundle(serverId, characterId, parsedBoardIds, lang);
        CachePolicyDto cache = new CachePolicyDto(false, true);
        return ApiResponse.success("OK", response, cache.cacheHit(), 0);
    }

    @GetMapping("/equipment/item")
    public ApiResponse<CharacterEquipmentItemResponse> getEquipmentItem(
        @RequestParam Long id,
        @RequestParam Integer enchantLevel,
        @RequestParam String characterId,
        @RequestParam String serverId,
        @RequestParam Integer slotPos,
        @RequestParam(defaultValue = "ko") String lang
    ) {
        CharacterEquipmentItemResponse response = characterEquipmentItemService.fetchItemDetail(
            id,
            enchantLevel,
            characterId,
            serverId,
            slotPos,
            lang
        );
        return ApiResponse.success("OK", response, response.cache().cacheHit(), 0);
    }
}
