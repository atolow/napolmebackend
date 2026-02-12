package com.dev.napolme.service.character;

import com.dev.napolme.domain.character.SavedCharacter;
import com.dev.napolme.dto.character.CharacterResponse;
import com.dev.napolme.dto.character.CharacterSummaryDto;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterInfoResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import com.dev.napolme.repository.character.SavedCharacterRepository;
import com.dev.napolme.util.Aion2UrlParser;
import java.time.Instant;
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

    private static final Logger log = LoggerFactory.getLogger(CharacterFetchService.class);
    private static final String LANG = "ko";

    private final PlayNcClient playNcClient;
    private final SavedCharacterRepository savedCharacterRepository;

    public CharacterFetchService(
        PlayNcClient playNcClient,
        SavedCharacterRepository savedCharacterRepository
    ) {
        this.playNcClient = playNcClient;
        this.savedCharacterRepository = savedCharacterRepository;
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
        log.info("Refreshed saved character: id={}, nickname={}", entity.getId(), entity.getNickname());
        return toResponse(entity);
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
        c.setGuild(extractGuildFromPlayNcResponse(raw));
        c.setProfileImage(normalizeProfileImage(p.profileImage()));
        c.setItemLevel(extractItemLevel(raw));
        c.setLastSyncedAt(Instant.now());
        return c;
    }

    private void updateFromPlayNcResponse(SavedCharacter entity, PlayNcCharacterInfoResponse raw) {
        PlayNcCharacterInfoResponse.Profile p = raw.profile();
        entity.setNickname(p.characterName());
        entity.setLevel(p.characterLevel() != null ? p.characterLevel() : 1);
        entity.setServerName(p.serverName());
        entity.setClassName(p.className());
        entity.setGuild(extractGuildFromPlayNcResponse(raw));
        entity.setProfileImage(normalizeProfileImage(p.profileImage()));
        entity.setItemLevel(extractItemLevel(raw));
        entity.setLastSyncedAt(Instant.now());
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
            c.getGuild(),
            c.getProfileImage(),
            c.getItemLevel(),
            c.getLastSyncedAt(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
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
            c.getItemLevel()
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
