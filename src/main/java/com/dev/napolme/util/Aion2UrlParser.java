package com.dev.napolme.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 아이온2 공식 사이트 캐릭터 URL에서 serverId, characterId 추출.
 * 예: https://aion2.plaync.com/ko-kr/characters/123/abc%3Dxyz
 */
public final class Aion2UrlParser {

    private static final Pattern CHARACTER_URL_PATTERN = Pattern.compile(
        "^https?://aion2\\.plaync\\.com(?:/[a-z]{2}-[a-z]{2})?/characters/(\\d+)/(.+?)/?$"
    );

    private Aion2UrlParser() {}

    public static ParsedCharacterUrl parse(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        var matcher = CHARACTER_URL_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String serverId = matcher.group(1);
            String characterId = decode(matcher.group(2));
            return new ParsedCharacterUrl(serverId, characterId);
        }
        return parseManually(trimmed);
    }

    private static ParsedCharacterUrl parseManually(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || !host.equalsIgnoreCase("aion2.plaync.com")) {
                return null;
            }
            String path = uri.getPath();
            if (path == null) {
                return null;
            }
            String[] segments = path.split("/");
            int idx = -1;
            for (int i = 0; i < segments.length; i++) {
                if ("characters".equalsIgnoreCase(segments[i])) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0 || idx + 2 >= segments.length) {
                return null;
            }
            String serverId = segments[idx + 1];
            String characterId = decode(segments[idx + 2]);
            if (!serverId.matches("\\d+")) {
                return null;
            }
            return new ParsedCharacterUrl(serverId, characterId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public record ParsedCharacterUrl(String serverId, String characterId) {}
}
