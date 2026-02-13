package com.dev.napolme.domain.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * URL로 가져와 저장한 캐릭터 정보. ID 조회·갱신에 사용.
 */
@Entity
@Table(
    name = "saved_character",
    indexes = {
        @Index(name = "idx_saved_character_server_character", columnList = "server_id, character_id"),
        @Index(name = "idx_saved_character_nickname", columnList = "nickname")
    },
    uniqueConstraints = @UniqueConstraint(columnNames = { "server_id", "character_id" })
)
public class SavedCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false, length = 20)
    private String serverId;

    @Column(name = "character_id", nullable = false, length = 100)
    private String characterId;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "server_name", length = 50)
    private String serverName;

    @Column(name = "class_name", length = 30)
    private String className;

    /** 종족: elyos(천족), asmo(마족) */
    @Column(name = "tribe", length = 10)
    private String tribe;

    @Column(length = 50)
    private String guild;

    @Column(name = "profile_image", length = 512)
    private String profileImage;

    @Column(name = "item_level")
    private Integer itemLevel;

    @Column(name = "napolme_point")
    private Integer napolmePoint;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTribe() {
        return tribe;
    }

    public void setTribe(String tribe) {
        this.tribe = tribe;
    }

    public String getGuild() {
        return guild;
    }

    public void setGuild(String guild) {
        this.guild = guild;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public Integer getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(Integer itemLevel) {
        this.itemLevel = itemLevel;
    }

    public Integer getNapolmePoint() {
        return napolmePoint;
    }

    public void setNapolmePoint(Integer napolmePoint) {
        this.napolmePoint = napolmePoint;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
