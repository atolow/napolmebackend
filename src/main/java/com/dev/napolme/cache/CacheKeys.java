package com.dev.napolme.cache;

public final class CacheKeys {
    private CacheKeys() {
    }

    public static String characterProfile(String serverId, String characterName) {
        return "character:" + serverId + ":" + characterName + ":profile";
    }

    public static String characterStats(String serverId, String characterName) {
        return "character:" + serverId + ":" + characterName + ":stats";
    }

    public static String characterLock(String serverId, String characterName) {
        return "lock:character:" + serverId + ":" + characterName;
    }
}
