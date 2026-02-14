package com.dev.napolme.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auto.block")
public class AutoBlockProperties {

    private boolean enabled = true;
    private int failureThreshold = 50;
    private int failureWindowSeconds = 300;
    private int patternThreshold = 30;
    private int patternWindowSeconds = 300;
    private int uaUniqueThreshold = 200;
    private int uaWindowSeconds = 300;
    private int blockSeconds = 1800;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getFailureWindowSeconds() {
        return failureWindowSeconds;
    }

    public void setFailureWindowSeconds(int failureWindowSeconds) {
        this.failureWindowSeconds = failureWindowSeconds;
    }

    public int getPatternThreshold() {
        return patternThreshold;
    }

    public void setPatternThreshold(int patternThreshold) {
        this.patternThreshold = patternThreshold;
    }

    public int getPatternWindowSeconds() {
        return patternWindowSeconds;
    }

    public void setPatternWindowSeconds(int patternWindowSeconds) {
        this.patternWindowSeconds = patternWindowSeconds;
    }

    public int getUaUniqueThreshold() {
        return uaUniqueThreshold;
    }

    public void setUaUniqueThreshold(int uaUniqueThreshold) {
        this.uaUniqueThreshold = uaUniqueThreshold;
    }

    public int getUaWindowSeconds() {
        return uaWindowSeconds;
    }

    public void setUaWindowSeconds(int uaWindowSeconds) {
        this.uaWindowSeconds = uaWindowSeconds;
    }

    public int getBlockSeconds() {
        return blockSeconds;
    }

    public void setBlockSeconds(int blockSeconds) {
        this.blockSeconds = blockSeconds;
    }
}
