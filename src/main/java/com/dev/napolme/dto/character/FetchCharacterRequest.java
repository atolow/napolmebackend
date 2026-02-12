package com.dev.napolme.dto.character;

import jakarta.validation.constraints.NotBlank;

public class FetchCharacterRequest {

    @NotBlank(message = "URL is required")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
