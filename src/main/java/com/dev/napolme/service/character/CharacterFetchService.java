package com.dev.napolme.service.character;

import com.dev.napolme.domain.character.SavedCharacter;
import com.dev.napolme.dto.character.CharacterResponse;
import com.dev.napolme.dto.character.CharacterSummaryDto;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterInfoResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import com.dev.napolme.repository.character.SavedCharacterRepository;
import com.dev.napolme.service.combat.CombatScoreService;
import com.dev.napolme.util.Aion2UrlParser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * URL로 캐릭터 가져오기·저장, ID 조회, 갱신.
 */
@Service
public class CharacterFetchService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Logger log = LoggerFactory.getLogger(CharacterFetchService.class);
    private static final String LANG = "ko";
    /** 정보 갱신 후 재갱신 가능까지 대기 시간(초). 서버가 쿨다운을 관리한다. */
    private static final int REFRESH_COOLDOWN_SECONDS = 60;

    private final PlayNcClient playNcClient;
    private final SavedCharacterRepository savedCharacterRepository;
    private final CombatScoreService combatScoreService;

    public CharacterFetchService(
        PlayNcClient playNcClient,
        SavedCharacterRepository savedCharacterRepository,
        CombatScoreService combatScoreService
    ) {
        this.playNcClient = playNcClient;
        this.savedCharacterRepository = savedCharacterRepository;
        this.combatScoreService = combatScoreService;
    }

    @Transactional
    public CharacterResponse fetchByUrl(String url) {
        Aion2UrlParser.ParsedCharacterUrl parsed = Aion2UrlParser.parse(url);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid character URL: " + url);
        }

        Optional<SavedCharacter> existing = savedCharacterRepository
            .findByServerIdAndCharacterId(parsed.serverId(), parsed.characterId());
        if (existing.isPresent()) {
            SavedCharacter c = existing.get();
            log.info("Found existing saved character: id={}, nickname={}", c.getId(), c.getNickname());
            return toResponse(c);
        }

        PlayNcCharacterInfoResponse raw = playNcClient.fetchCharacterInfo(
            parsed.serverId(),
            parsed.characterId(),
            LANG
        );
        if (raw == null || raw.profile() == null) {
            throw new IllegalArgumentException(
                "Character not found in official API: serverId=" + parsed.serverId()
                    + ", characterId=" + parsed.characterId()
            );
        }

        SavedCharacter entity = fromPlayNcResponse(raw, parsed.serverId(), parsed.characterId());
        entity = savedCharacterRepository.save(entity);
        updateNapolmePointIfPossible(entity);
        entity = savedCharacterRepository.save(entity);
        log.info("Saved new character from URL: id={}, nickname={}", entity.getId(), entity.getNickname());
        return toResponse(entity);
    }

    /** serverId + characterId 로 저장 (없으면 공식 API에서 조회 후 저장). 상세 페이지에서 갱신 시 사용. */
    @Transactional
    public CharacterResponse fetchByRef(String serverId, String characterId) {
        if (serverId == null || characterId == null || characterId.isBlank()) {
            throw new IllegalArgumentException("serverId and characterId are required");
        }
        Optional<SavedCharacter> existing = savedCharacterRepository
            .findByServerIdAndCharacterId(serverId, characterId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        PlayNcCharacterInfoResponse raw = playNcClient.fetchCharacterInfo(serverId, characterId, LANG);
        if (raw == null || raw.profile() == null) {
            throw new IllegalArgumentException(
                "Character not found in official API: serverId=" + serverId + ", characterId=" + characterId
            );
        }
        SavedCharacter entity = fromPlayNcResponse(raw, serverId, characterId);
        entity = savedCharacterRepository.save(entity);
        updateNapolmePointIfPossible(entity);
        entity = savedCharacterRepository.save(entity);
        log.info("Saved character by ref: id={}, nickname={}", entity.getId(), entity.getNickname());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Optional<CharacterResponse> getById(Long id) {
        return savedCharacterRepository.findById(id).map(this::toResponse);
    }

    /** serverId + characterId 로 저장된 캐릭터 조회 (상세 페이지에서 갱신 버튼 노출용) */
    @Transactional(readOnly = true)
    public Optional<CharacterResponse> getByServerIdAndCharacterId(String serverId, String characterId) {
        if (serverId == null || characterId == null || characterId.isBlank()) {
            return Optional.empty();
        }
        return savedCharacterRepository.findByServerIdAndCharacterId(serverId, characterId)
            .map(this::toResponse);
    }

    /** 마지막 갱신 시각(lastSyncedAt) 기준 남은 쿨다운(초). 페이지 로드 시 서버가 알려주면 새로고침 후에도 유지된다. */
    @Transactional(readOnly = true)
    public int getRemainingRefreshCooldownSeconds(Long savedCharacterId) {
        return savedCharacterRepository.findById(savedCharacterId)
            .map(this::remainingCooldownFrom)
            .orElse(0);
    }

    @Transactional(readOnly = true)
    public int getRemainingRefreshCooldownSeconds(String serverId, String characterId) {
        if (serverId == null || characterId == null || characterId.isBlank()) {
            return 0;
        }
        return savedCharacterRepository.findByServerIdAndCharacterId(serverId, characterId)
            .map(this::remainingCooldownFrom)
            .orElse(0);
    }

    private int remainingCooldownFrom(SavedCharacter c) {
        LocalDateTime last = c.getLastSyncedAt();
        if (last == null) {
            return 0;
        }
        long elapsed = Instant.now().getEpochSecond() - last.atZone(SEOUL).toInstant().getEpochSecond();
        return (int) Math.max(0, REFRESH_COOLDOWN_SECONDS - elapsed);
    }

    @Transactional
    public CharacterResponse refresh(Long id) {
        SavedCharacter entity = savedCharacterRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Character not found: id=" + id));

        PlayNcCharacterInfoResponse raw = playNcClient.fetchCharacterInfo(
            entity.getServerId(),
            entity.getCharacterId(),
            LANG
        );
        if (raw == null || raw.profile() == null) {
            throw new IllegalStateException(
                "Failed to fetch character from official API: serverId=" + entity.getServerId()
                    + ", characterId=" + entity.getCharacterId()
            );
        }

        updateFromPlayNcResponse(entity, raw);
        entity = savedCharacterRepository.save(entity);
        updateNapolmePointIfPossible(entity);
        entity = savedCharacterRepository.save(entity);
        log.info("Refreshed saved character: id={}, nickname={}", entity.getId(), entity.getNickname());
        return toResponse(entity);
    }

    /** 나폴미 점수 계산 후 저장. 실패 시 로그만 남기고 기존 값 유지. */
    private void updateNapolmePointIfPossible(SavedCharacter entity) {
        try {
            var result = combatScoreService.calculateFromCharacter(
                entity.getServerId(),
                entity.getCharacterId(),
                null,
                null
            );
            long total = result.totalCombatPower();
            entity.setNapolmePoint(total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);
        } catch (Exception e) {
            log.warn("Failed to calculate napolme point for character id={}: {}", entity.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<CharacterSummaryDto> searchByNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return List.of();
        }
        return savedCharacterRepository.findByNicknameContainingIgnoreCase(nickname)
            .stream()
            .map(this::toSummaryDto)
            .toList();
    }

    private SavedCharacter fromPlayNcResponse(
        PlayNcCharacterInfoResponse raw,
        String serverId,
        String characterId
    ) {
        PlayNcCharacterInfoResponse.Profile p = raw.profile();
        SavedCharacter c = new SavedCharacter();
        c.setServerId(serverId);
        c.setCharacterId(characterId);
        c.setNickname(p.characterName());
        c.setLevel(p.characterLevel() != null ? p.characterLevel() : 1);
        c.setServerName(p.serverName());
        c.setClassName(p.className());
        c.setTribe(tribeFromRaceId(p.raceId()));
        c.setGuild(extractGuildFromPlayNcResponse(raw));
        c.setProfileImage(normalizeProfileImage(p.profileImage()));
        c.setItemLevel(extractItemLevel(raw));
        c.setLastSyncedAt(LocalDateTime.now(SEOUL));
        return c;
    }

    private void updateFromPlayNcResponse(SavedCharacter entity, PlayNcCharacterInfoResponse raw) {
        PlayNcCharacterInfoResponse.Profile p = raw.profile();
        entity.setNickname(p.characterName());
        entity.setLevel(p.characterLevel() != null ? p.characterLevel() : 1);
        entity.setServerName(p.serverName());
        entity.setClassName(p.className());
        entity.setTribe(tribeFromRaceId(p.raceId()));
        entity.setGuild(extractGuildFromPlayNcResponse(raw));
        entity.setProfileImage(normalizeProfileImage(p.profileImage()));
        entity.setItemLevel(extractItemLevel(raw));
        entity.setLastSyncedAt(LocalDateTime.now(SEOUL));
    }

    /** PlayNC raceId → tribe: 1=천족(elyos), 2=마족(asmo) */
    private String tribeFromRaceId(Integer raceId) {
        if (raceId == null) return null;
        return switch (raceId) {
            case 1 -> "elyos";
            case 2 -> "asmo";
            default -> null;
        };
    }

    /** PlayNC 응답에서 길드명 추출 (ranking 목록의 첫 번째 non-null guildName 사용) */
    private String extractGuildFromPlayNcResponse(PlayNcCharacterInfoResponse raw) {
        if (raw.ranking() == null || raw.ranking().rankingList() == null) {
            return null;
        }
        for (PlayNcCharacterInfoResponse.RankingItem item : raw.ranking().rankingList()) {
            if (item != null && item.guildName() != null && !item.guildName().isBlank()) {
                return item.guildName();
            }
        }
        return null;
    }

    private Integer extractItemLevel(PlayNcCharacterInfoResponse raw) {
        if (raw.stat() == null || raw.stat().statList() == null) {
            return null;
        }
        for (PlayNcCharacterInfoResponse.StatItem item : raw.stat().statList()) {
            if (item != null && "ItemLevel".equals(item.type())) {
                return item.value();
            }
        }
        return null;
    }

    private String normalizeProfileImage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("http")) {
            return value;
        }
        return "https://profileimg.plaync.com" + value;
    }

    private CharacterResponse toResponse(SavedCharacter c) {
        return new CharacterResponse(
            c.getId(),
            c.getCharacterId(),
            c.getServerId(),
            c.getNickname(),
            c.getLevel(),
            c.getServerName(),
            c.getClassName(),
            c.getTribe(),
            c.getGuild(),
            c.getProfileImage(),
            c.getItemLevel(),
            c.getNapolmePoint(),
            toInstant(c.getLastSyncedAt()),
            toInstant(c.getCreatedAt()),
            toInstant(c.getUpdatedAt())
        );
    }

    private static Instant toInstant(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(SEOUL).toInstant();
    }

    private CharacterSummaryDto toSummaryDto(SavedCharacter c) {
        return new CharacterSummaryDto(
            c.getCharacterId(),
            c.getNickname(),
            c.getLevel(),
            parseServerId(c.getServerId()),
            c.getServerName(),
            null,
            null,
            c.getProfileImage(),
            c.getItemLevel(),
            c.getTribe()
        );
    }

    private Integer parseServerId(String serverId) {
        try {
            return serverId == null ? null : Integer.parseInt(serverId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
