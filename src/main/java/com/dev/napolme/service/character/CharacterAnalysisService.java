package com.dev.napolme.service.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import com.dev.napolme.dto.character.CharacterInfoResponse;
import com.dev.napolme.dto.character.CharacterInfoResponse.DaevanionBoardItem;
import com.dev.napolme.dto.character.CharacterInfoResponse.RankingItem;
import com.dev.napolme.dto.character.CharacterInfoResponse.StatItem;
import com.dev.napolme.dto.character.CharacterInfoResponse.StatsSummary;
import com.dev.napolme.dto.character.CharacterInfoResponse.TitleItem;
import com.dev.napolme.dto.plaync.character.PlayNcCharacterInfoResponse;
import com.dev.napolme.infra.plaync.PlayNcClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CharacterAnalysisService {
    private static final int TOP_TITLE_COUNT = 3;
    private static final Set<String> PRIMARY_STAT_TYPES = buildPrimaryTypes();

    private final PlayNcClient playNcClient;

    public CharacterAnalysisService(PlayNcClient playNcClient) {
        this.playNcClient = playNcClient;
    }

    public CharacterInfoResponse fetchCharacterInfo(
        String serverId,
        String characterId,
        String lang
    ) {
        PlayNcCharacterInfoResponse raw = playNcClient.fetchCharacterInfo(serverId, characterId, lang);
        if (raw == null || raw.profile() == null) {
            CachePolicyDto cache = new CachePolicyDto(false, true);
            return new CharacterInfoResponse(
                characterId,
                null,
                null,
                toInteger(serverId),
                null,
                null,
                null,
                null,
                null,
                null,
                new StatsSummary(null, List.of(), List.of()),
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                cache
            );
        }

        CachePolicyDto cache = new CachePolicyDto(false, true);
        StatsSummary statsSummary = summarizeStats(raw.stat());
        List<RankingItem> rankings = summarizeRankings(raw.ranking());
        List<TitleItem> topTitles = summarizeTitles(raw.title());
        List<DaevanionBoardItem> boards = summarizeBoards(raw.daevanion());

        return new CharacterInfoResponse(
            raw.profile().characterId(),
            raw.profile().characterName(),
            raw.profile().characterLevel(),
            raw.profile().serverId(),
            raw.profile().serverName(),
            raw.profile().regionName(),
            raw.profile().className(),
            raw.profile().raceName(),
            raw.profile().genderName(),
            raw.profile().profileImage(),
            statsSummary,
            rankings,
            topTitles,
            boards,
            Instant.now(),
            cache
        );
    }

    private StatsSummary summarizeStats(PlayNcCharacterInfoResponse.Stat stat) {
        if (stat == null || stat.statList() == null) {
            return new StatsSummary(null, List.of(), List.of());
        }
        Integer itemLevel = null;
        List<StatItem> primary = new ArrayList<>();
        List<StatItem> special = new ArrayList<>();
        for (PlayNcCharacterInfoResponse.StatItem item : stat.statList()) {
            if (item == null) {
                continue;
            }
            if ("ItemLevel".equals(item.type())) {
                itemLevel = item.value();
                continue;
            }
            StatItem mapped = new StatItem(
                item.type(),
                item.name(),
                item.value(),
                item.statSecondList()
            );
            if (PRIMARY_STAT_TYPES.contains(item.type())) {
                primary.add(mapped);
            } else {
                special.add(mapped);
            }
        }
        return new StatsSummary(itemLevel, primary, special);
    }

    private List<RankingItem> summarizeRankings(PlayNcCharacterInfoResponse.Ranking ranking) {
        if (ranking == null || ranking.rankingList() == null) {
            return List.of();
        }
        List<RankingItem> items = new ArrayList<>();
        for (PlayNcCharacterInfoResponse.RankingItem item : ranking.rankingList()) {
            if (item == null || item.rank() == null) {
                continue;
            }
            items.add(new RankingItem(
                item.rankingContentsName(),
                item.rank(),
                item.point(),
                item.prevRank(),
                item.rankChange(),
                item.className(),
                item.guildName()
            ));
        }
        return items;
    }

    private List<TitleItem> summarizeTitles(PlayNcCharacterInfoResponse.Title title) {
        if (title == null || title.titleList() == null) {
            return List.of();
        }
        List<TitleItem> items = new ArrayList<>();
        int count = 0;
        for (PlayNcCharacterInfoResponse.TitleItem item : title.titleList()) {
            if (item == null) {
                continue;
            }
            items.add(new TitleItem(
                item.id(),
                item.equipCategory(),
                item.name(),
                item.grade(),
                item.totalCount(),
                item.ownedCount(),
                item.ownedPercent(),
                mapTitleStats(item.statList()),
                mapTitleStats(item.equipStatList())
            ));
            count++;
            if (count >= TOP_TITLE_COUNT) {
                break;
            }
        }
        return items;
    }

    private List<String> mapTitleStats(List<PlayNcCharacterInfoResponse.StatDesc> stats) {
        if (stats == null) {
            return List.of();
        }
        List<String> mapped = new ArrayList<>();
        for (PlayNcCharacterInfoResponse.StatDesc stat : stats) {
            if (stat != null && stat.desc() != null) {
                mapped.add(stat.desc());
            }
        }
        return mapped;
    }

    private List<DaevanionBoardItem> summarizeBoards(PlayNcCharacterInfoResponse.Daevanion daevanion) {
        if (daevanion == null || daevanion.boardList() == null) {
            return List.of();
        }
        List<DaevanionBoardItem> items = new ArrayList<>();
        for (PlayNcCharacterInfoResponse.DaevanionBoard board : daevanion.boardList()) {
            if (board == null) {
                continue;
            }
            items.add(new DaevanionBoardItem(
                board.id(),
                board.name(),
                board.totalNodeCount(),
                board.openNodeCount(),
                board.openPercent(),
                board.icon()
            ));
        }
        return items;
    }

    private Integer toInteger(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Set<String> buildPrimaryTypes() {
        Set<String> types = new HashSet<>();
        types.add("STR");
        types.add("DEX");
        types.add("INT");
        types.add("CON");
        types.add("AGI");
        types.add("WIS");
        return types;
    }
}
