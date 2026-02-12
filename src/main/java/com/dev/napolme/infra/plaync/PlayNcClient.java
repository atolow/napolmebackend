package com.dev.napolme.infra.plaync;

import com.dev.napolme.dto.plaync.character.PlayNcCharacterDaevanionDetailResponse;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterEquipmentItemResponse;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterEquipmentSkillResponse;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterInfoResponse;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterSearchResponse;
import com.dev.napolme.dto.plaync.board.PlayNcBoardUpdateResponse;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PlayNcClient {
    private static final Logger log = LoggerFactory.getLogger(PlayNcClient.class);
    private final RestClient restClient;
    private final PlayNcProperties properties;

    public PlayNcClient(RestClient playNcRestClient, PlayNcProperties properties) {
        this.restClient = playNcRestClient;
        this.properties = properties;
    }

    public PlayNcCharacterInfoResponse fetchCharacterInfo(
        String serverId,
        String characterId,
        String lang
    ) {
        try {
            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(properties.getCharacterInfoPath())
                    .queryParam("lang", lang)
                    .queryParam("characterId", characterId)
                    .queryParam("serverId", serverId)
                    .build()
                )
                .retrieve()
                .body(PlayNcCharacterInfoResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn(
                "PlayNC info failed status={} serverId={} characterId={} body={}",
                ex.getStatusCode().value(),
                serverId,
                characterId,
                ex.getResponseBodyAsString()
            );
            throw ex;
        }
    }

    public PlayNcCharacterEquipmentSkillResponse fetchCharacterEquipmentSkill(
        String serverId,
        String characterId,
        String lang
    ) {
        try {
            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(properties.getCharacterEquipmentPath())
                    .queryParam("lang", lang)
                    .queryParam("characterId", characterId)
                    .queryParam("serverId", serverId)
                    .build()
                )
                .retrieve()
                .body(PlayNcCharacterEquipmentSkillResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn(
                "PlayNC equipment failed status={} serverId={} characterId={} body={}",
                ex.getStatusCode().value(),
                serverId,
                characterId,
                ex.getResponseBodyAsString()
            );
            throw ex;
        }
    }

    public PlayNcCharacterSearchResponse fetchCharacterSearch(
        String keyword,
        Integer race,
        String serverId,
        int page,
        int size
    ) {
        try {
            return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                        .path(properties.getCharacterSearchPath())
                        .queryParam("keyword", keyword)
                        .queryParam("page", page)
                        .queryParam("size", size);
                    if (race != null) {
                        builder.queryParam("race", race);
                    }
                    if (serverId != null && !serverId.isBlank()) {
                        builder.queryParam("serverId", serverId);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(PlayNcCharacterSearchResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn(
                "PlayNC search failed status={} keyword={} race={} serverId={} body={}",
                ex.getStatusCode().value(),
                keyword,
                race,
                serverId,
                ex.getResponseBodyAsString()
            );
            throw ex;
        }
    }

    public PlayNcCharacterEquipmentItemResponse fetchCharacterEquipmentItem(
        Long id,
        Integer enchantLevel,
        String characterId,
        String serverId,
        Integer slotPos,
        String lang
    ) {
        try {
            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(properties.getCharacterEquipmentItemPath())
                    .queryParam("id", id)
                    .queryParam("enchantLevel", enchantLevel)
                    .queryParam("characterId", characterId)
                    .queryParam("serverId", serverId)
                    .queryParam("slotPos", slotPos)
                    .queryParam("lang", lang)
                    .build()
                )
                .retrieve()
                .body(PlayNcCharacterEquipmentItemResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn(
                "PlayNC equipment item failed status={} id={} serverId={} characterId={} body={}",
                ex.getStatusCode().value(),
                id,
                serverId,
                characterId,
                ex.getResponseBodyAsString()
            );
            throw ex;
        }
    }

    public PlayNcCharacterDaevanionDetailResponse fetchCharacterDaevanionDetail(
        String serverId,
        String characterId,
        Integer boardId,
        String lang
    ) {
        try {
            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(properties.getCharacterDaevanionDetailPath())
                    .queryParam("lang", lang)
                    .queryParam("characterId", characterId)
                    .queryParam("serverId", serverId)
                    .queryParam("boardId", boardId)
                    .build()
                )
                .retrieve()
                .body(PlayNcCharacterDaevanionDetailResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn(
                "PlayNC daevanion detail failed status={} serverId={} characterId={} boardId={} body={}",
                ex.getStatusCode().value(),
                serverId,
                characterId,
                boardId,
                ex.getResponseBodyAsString()
            );
            throw ex;
        }
    }

    public PlayNcBoardUpdateResponse fetchBoardUpdateArticles(int size) {
        String url = properties.getCommunityBaseUrl() + properties.getBoardUpdateArticlePath();
        URI uri = UriComponentsBuilder.fromUriString(url)
            .queryParam("isVote", "true")
            .queryParam("moreSize", size)
            .queryParam("moreDirection", "BEFORE")
            .queryParam("previousArticleId", "0")
            .build()
            .toUri();
        try {
            return restClient.get()
                .uri(uri)
                .headers(headers -> headers.set(
                    HttpHeaders.REFERER,
                    "https://aion2.plaync.com/ko-kr/board/update/list"
                ))
                .retrieve()
                .body(PlayNcBoardUpdateResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn(
                "PlayNC board update articles failed status={} size={} body={}",
                ex.getStatusCode().value(),
                size,
                ex.getResponseBodyAsString()
            );
            throw ex;
        }
    }
}
