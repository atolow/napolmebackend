package com.dev.napolme.domain.logging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 캐릭터 검색 시 검색어 기록. 일일 검색 랭킹 집계용.
 * searched_at은 한국 시간(Asia/Seoul) 기준으로 저장.
 */
@Entity
@Table(name = "search_logs")
public class SearchLog {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_name", nullable = false, length = 100)
    private String characterName;

    /** 종족: elyos(천족), asmo(마족). 검색 결과가 있을 때만 기록. */
    @Column(name = "tribe", length = 10)
    private String tribe;

    /** 검색 시 선택한 서버 ID (해당 서버에서 검색했을 때만 기록). */
    @Column(name = "server_id", length = 10)
    private String serverId;

    @Column(name = "searched_at", nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime searchedAt;

    @PrePersist
    private void onCreate() {
        if (this.searchedAt == null) {
            this.searchedAt = LocalDateTime.now(SEOUL);
        }
    }

    protected SearchLog() {}

    public SearchLog(String characterName) {
        this.characterName = characterName;
        this.searchedAt = LocalDateTime.now(SEOUL);
    }

    public SearchLog(String characterName, String tribe) {
        this.characterName = characterName;
        this.tribe = tribe;
        this.searchedAt = LocalDateTime.now(SEOUL);
    }

    public SearchLog(String characterName, String tribe, String serverId) {
        this.characterName = characterName;
        this.tribe = tribe;
        this.serverId = serverId;
        this.searchedAt = LocalDateTime.now(SEOUL);
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

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }
}
