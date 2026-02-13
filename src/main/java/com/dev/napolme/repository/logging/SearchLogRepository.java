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
     * 당일(자정 UTC 기준) 검색 로그에서 캐릭터명별 건수 상위 10개.
     */
    @Query(value = """
        SELECT s.character_name AS name, MAX(s.tribe) AS tribe, COUNT(*) AS cnt
        FROM search_logs s
        WHERE s.searched_at >= :dayStart AND s.searched_at < :dayEnd
        GROUP BY s.character_name
        ORDER BY cnt DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findDailyTop10(@Param("dayStart") Instant dayStart, @Param("dayEnd") Instant dayEnd);
}
