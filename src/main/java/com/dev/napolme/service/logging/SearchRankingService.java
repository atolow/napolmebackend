package com.dev.napolme.service.logging;

import com.dev.napolme.domain.logging.SearchLog;
import com.dev.napolme.repository.logging.SearchLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 검색 로그 기록 및 일일 검색 랭킹 조회.
 * ddl-auto=create 등으로 어제 데이터가 없어도, "이전 조회 시점 랭킹"과 비교해 N▲/N▼ 표시.
 */
@Service
public class SearchRankingService {

    private final SearchLogRepository searchLogRepository;

    /** 이전에 조회했을 때의 (이름|서버ID) → 순위(1-based). 다음 조회 시 순위 변동 계산에 사용. */
    private final Map<String, Integer> previousRank = new ConcurrentHashMap<>();
    /** 직전 계산에 사용한 랭킹 스냅샷 fingerprint(중복 요청 시 같은 결과 반환용). */
    private volatile String lastRankingFingerprint = null;
    /** 마지막으로 계산한 응답(동일 스냅샷 재요청 시 재사용). */
    private volatile List<DailySearchRankItem> lastComputedResult = List.of();

    public SearchRankingService(SearchLogRepository searchLogRepository) {
        this.searchLogRepository = searchLogRepository;
    }

    /** 동기 처리로 검색 응답 전에 DB 반영 → 직후 일일 랭킹 조회 시 포함됨 */
    @Transactional
    public void recordSearch(String characterName, String tribe, String serverId) {
        if (characterName == null || characterName.isBlank()) {
            return;
        }
        String trimmed = characterName.trim();
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        if (tribe != null && (tribe.equals("elyos") || tribe.equals("asmo"))) {
            searchLogRepository.save(new SearchLog(trimmed, tribe, serverId));
        } else {
            searchLogRepository.save(new SearchLog(trimmed));
        }
    }

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /**
     * 당일(한국 시간) 검색 횟수 상위 10건.
     * 이전에 조회한 랭킹과 비교해 up/down/changeAmount 계산 후, 이번 결과를 다음 비교용으로 저장.
     */
    @Transactional(readOnly = true)
    public synchronized List<DailySearchRankItem> getDailyTop10() {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        List<Object[]> currentRows = searchLogRepository.findDailyTop10(
            dayStart, dayEnd, PageRequest.of(0, 10)
        );
        String currentFingerprint = buildFingerprint(currentRows);

        // React StrictMode 등으로 동일 요청이 연속 호출되면
        // 첫 응답의 N▲/N▼가 두 번째 응답에서 same으로 덮이지 않도록 캐시된 결과를 그대로 반환한다.
        if (lastRankingFingerprint != null && lastRankingFingerprint.equals(currentFingerprint)) {
            return lastComputedResult;
        }

        List<DailySearchRankItem> result = buildWithRankChange(currentRows, previousRank);

        previousRank.clear();
        for (int i = 0; i < currentRows.size(); i++) {
            Object[] row = currentRows.get(i);
            String key = rankKey(rowName(row), rowServerId(row));
            previousRank.put(key, i + 1);
        }

        lastRankingFingerprint = currentFingerprint;
        lastComputedResult = List.copyOf(result);
        return lastComputedResult;
    }

    private static String rankKey(String name, String serverId) {
        String n = name != null ? name.trim() : "";
        String s = (serverId != null && !serverId.isBlank()) ? String.valueOf(serverId).trim() : "";
        return n + "|" + s;
    }

    private static String rowServerId(Object[] row) {
        if (row.length <= 1 || row[1] == null) return null;
        Object v = row[1];
        String s = v instanceof String ? (String) v : String.valueOf(v);
        return s.isBlank() ? null : s.trim();
    }

    private static String rowName(Object[] row) {
        Object v = row[0];
        return v == null ? "" : (v instanceof String ? (String) v : String.valueOf(v)).trim();
    }

    private String buildFingerprint(List<Object[]> rows) {
        StringBuilder sb = new StringBuilder(rows.size() * 32);
        for (Object[] row : rows) {
            String key = rankKey(rowName(row), rowServerId(row));
            long count = row.length > 3 && row[3] instanceof Number ? ((Number) row[3]).longValue() : 0L;
            sb.append(key).append(':').append(count).append(';');
        }
        return sb.toString();
    }

    private List<DailySearchRankItem> buildWithRankChange(
        List<Object[]> currentRows,
        Map<String, Integer> previousRankMap
    ) {
        List<DailySearchRankItem> result = new ArrayList<>();
        for (int i = 0; i < currentRows.size(); i++) {
            Object[] row = currentRows.get(i);
            String name = rowName(row);
            String rawServerId = rowServerId(row);
            String serverId = (rawServerId == null || rawServerId.isBlank()) ? null : rawServerId;
            String tribe = row.length > 2 && row[2] != null ? String.valueOf(row[2]) : null;
            long count = row.length > 3 && row[3] instanceof Number ? ((Number) row[3]).longValue() : 0L;
            int currentRank = i + 1;
            String key = rankKey(name, rawServerId);
            Integer prevRank = previousRankMap.get(key);
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
            result.add(new DailySearchRankItem(name, count, rankChange, changeAmount, tribe, serverId));
        }
        return result;
    }

    public record DailySearchRankItem(String name, long count, String rankChange, int changeAmount, String tribe, String serverId) {}
}
