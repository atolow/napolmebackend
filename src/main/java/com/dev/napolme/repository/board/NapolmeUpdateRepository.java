package com.dev.napolme.repository.board;

import com.dev.napolme.domain.board.NapolmeUpdate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NapolmeUpdateRepository extends JpaRepository<NapolmeUpdate, Long> {

    List<NapolmeUpdate> findAllByOrderByCreatedAtDesc();
}
