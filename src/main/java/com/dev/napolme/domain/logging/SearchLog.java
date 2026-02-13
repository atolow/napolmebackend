package com.dev.napolme.domain.logging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 캐릭터 검색 시 검색어 기록. 일일 검색 랭킹 집계용.
 */
@Entity
@Table(name = "search_logs")
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_name", nullable = false, length = 100)
    private String characterName;

    /** 종족: elyos(천족), asmo(마족). 검색 결과가 있을 때만 기록. */
    @Column(name = "tribe", length = 10)
    private String tribe;

    @Column(name = "searched_at", nullable = false, updatable = false)
    private Instant searchedAt;

    @PrePersist
    private void onCreate() {
        if (this.searchedAt == null) {
            this.searchedAt = Instant.now();
        }
    }

    protected SearchLog() {}

    public SearchLog(String characterName) {
        this.characterName = characterName;
        this.searchedAt = Instant.now();
    }

    public SearchLog(String characterName, String tribe) {
        this.characterName = characterName;
        this.tribe = tribe;
        this.searchedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getTribe() {
        return tribe;
    }

    public void setTribe(String tribe) {
        this.tribe = tribe;
    }

    public Instant getSearchedAt() {
        return searchedAt;
    }
}
