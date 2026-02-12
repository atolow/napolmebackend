package com.dev.napolme.infra.plaync;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plaync")
public class PlayNcProperties {
    private String baseUrl;
    private String characterInfoPath = "/api/character/info";
    private String characterEquipmentPath = "/api/character/equipment";
    private String characterSearchPath = "/ko-kr/api/search/aion2/search/v2/character";
    private String characterEquipmentItemPath = "/api/character/equipment/item";
    private String characterDaevanionDetailPath = "/api/character/daevanion/detail";
    private String boardUpdateListPath = "/api/board/update/list";
    private String communityBaseUrl = "https://api-community.plaync.com";
    private String boardUpdateArticlePath =
        "/aion2/board/update_ko/article/search/moreArticle";
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public String getCharacterInfoPath() {
        return characterInfoPath;
    }

    public void setCharacterInfoPath(String characterInfoPath) {
        this.characterInfoPath = characterInfoPath;
    }

    public String getCharacterEquipmentPath() {
        return characterEquipmentPath;
    }

    public void setCharacterEquipmentPath(String characterEquipmentPath) {
        this.characterEquipmentPath = characterEquipmentPath;
    }

    public String getCharacterSearchPath() {
        return characterSearchPath;
    }

    public void setCharacterSearchPath(String characterSearchPath) {
        this.characterSearchPath = characterSearchPath;
    }

    public String getCharacterEquipmentItemPath() {
        return characterEquipmentItemPath;
    }

    public void setCharacterEquipmentItemPath(String characterEquipmentItemPath) {
        this.characterEquipmentItemPath = characterEquipmentItemPath;
    }

    public String getCharacterDaevanionDetailPath() {
        return characterDaevanionDetailPath;
    }

    public void setCharacterDaevanionDetailPath(String characterDaevanionDetailPath) {
        this.characterDaevanionDetailPath = characterDaevanionDetailPath;
    }

    public String getBoardUpdateListPath() {
        return boardUpdateListPath;
    }

    public void setBoardUpdateListPath(String boardUpdateListPath) {
        this.boardUpdateListPath = boardUpdateListPath;
    }

    public String getCommunityBaseUrl() {
        return communityBaseUrl;
    }

    public void setCommunityBaseUrl(String communityBaseUrl) {
        this.communityBaseUrl = communityBaseUrl;
    }

    public String getBoardUpdateArticlePath() {
        return boardUpdateArticlePath;
    }

    public void setBoardUpdateArticlePath(String boardUpdateArticlePath) {
        this.boardUpdateArticlePath = boardUpdateArticlePath;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
