package com.dev.napolme.repository.logging;

import com.dev.napolme.domain.logging.SearchLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    /**
     * 당일(한국 시간) 검색 로그에서 (캐릭터명, 서버ID)별 건수 상위 10개.
     * JPQL 사용으로 LocalDateTime 바인딩이 엔티티와 동일하게 동작(500 방지).
     */
    @Query("""
        SELECT s.characterName, s.serverId, MAX(s.tribe), COUNT(s)
        FROM SearchLog s
        WHERE s.searchedAt >= :dayStart AND s.searchedAt < :dayEnd
        GROUP BY s.characterName, s.serverId
        ORDER BY COUNT(s) DESC
        """)
    List<Object[]> findDailyTop10(
        @Param("dayStart") LocalDateTime dayStart,
        @Param("dayEnd") LocalDateTime dayEnd,
        Pageable pageable
    );
}
