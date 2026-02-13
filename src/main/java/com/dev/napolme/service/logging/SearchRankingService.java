package com.dev.napolme.service.logging;

import com.dev.napolme.domain.logging.SearchLog;
import com.dev.napolme.repository.logging.SearchLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 검색 로그 기록 및 일일 검색 랭킹 조회.
 */
@Service
public class SearchRankingService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final SearchLogRepository searchLogRepository;

    public SearchRankingService(SearchLogRepository searchLogRepository) {
        this.searchLogRepository = searchLogRepository;
    }

    /**
     * 검색 시 호출. 검색어(캐릭터명) 기록. 비동기로 저장.
     * @param tribe elyos(천족) 또는 asmo(마족). 검색 결과가 있을 때만 전달.
     */
    @Async
    @Transactional
    public void recordSearch(String characterName, String tribe) {
        if (characterName == null || characterName.isBlank()) {
            return;
        }
        String trimmed = characterName.trim();
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        if (tribe != null && (tribe.equals("elyos") || tribe.equals("asmo"))) {
            searchLogRepository.save(new SearchLog(trimmed, tribe));
        } else {
            searchLogRepository.save(new SearchLog(trimmed));
        }
    }

    /**
     * 당일(한국 기준) 검색 횟수 상위 10명. name, count, 전일 대비 순위 변동(up/down/same/new).
     */
    @Transactional(readOnly = true)
    public List<DailySearchRankItem> getDailyTop10() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate yesterday = today.minusDays(1);
        Instant todayStart = today.atStartOfDay(ZONE).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(ZONE).toInstant();
        Instant yesterdayStart = yesterday.atStartOfDay(ZONE).toInstant();
        Instant yesterdayEnd = todayStart;

        List<Object[]> todayRows = searchLogRepository.findDailyTop10(todayStart, todayEnd);
        List<Object[]> yesterdayRows = searchLogRepository.findDailyTop10(yesterdayStart, yesterdayEnd);

        Map<String, Integer> yesterdayRank = new HashMap<>();
        for (int i = 0; i < yesterdayRows.size(); i++) {
            yesterdayRank.put((String) yesterdayRows.get(i)[0], i + 1);
        }

        return buildWithRankChange(todayRows, yesterdayRank);
    }

    private List<DailySearchRankItem> buildWithRankChange(
        List<Object[]> todayRows,
        Map<String, Integer> yesterdayRank
    ) {
        List<DailySearchRankItem> result = new ArrayList<>();
        for (int i = 0; i < todayRows.size(); i++) {
            Object[] row = todayRows.get(i);
            String name = (String) row[0];
            String tribe = row.length > 2 && row[1] != null ? (String) row[1] : null;
            long count = ((Number) row[row.length > 2 ? 2 : 1]).longValue();
            int currentRank = i + 1;
            Integer prevRank = yesterdayRank.get(name);
            String rankChange;
            int changeAmount = 0;
            if (prevRank == null) {
                rankChange = "new";
            } else if (currentRank < prevRank) {
                rankChange = "up";
                changeAmount = prevRank - currentRank;
            } else if (currentRank > prevRank) {
                rankChange = "down";
                changeAmount = currentRank - prevRank;
            } else {
                rankChange = "same";
            }
            result.add(new DailySearchRankItem(name, count, rankChange, changeAmount, tribe));
        }
        return result;
    }

    public record DailySearchRankItem(String name, long count, String rankChange, int changeAmount, String tribe) {}
}
