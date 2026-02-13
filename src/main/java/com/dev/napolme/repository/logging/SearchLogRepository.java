package com.dev.napolme.repository.logging;

import com.dev.napolme.domain.logging.SearchLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    /**
     * 당일(한국 시간 기준) 검색 로그에서 (캐릭터명, 서버ID)별 건수 상위 10개.
     * server_id가 있는 검색만 포함 (해당 서버로 이동 가능한 항목만 랭킹에 노출).
     */
    @Query(value = """
        SELECT s.character_name AS name, s.server_id AS server_id, MAX(s.tribe) AS tribe, COUNT(*) AS cnt
        FROM search_logs s
        WHERE s.searched_at >= :dayStart AND s.searched_at < :dayEnd
          AND s.server_id IS NOT NULL AND TRIM(s.server_id) <> ''
        GROUP BY s.character_name, s.server_id
        ORDER BY cnt DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findDailyTop10(@Param("dayStart") Instant dayStart, @Param("dayEnd") Instant dayEnd);
}
