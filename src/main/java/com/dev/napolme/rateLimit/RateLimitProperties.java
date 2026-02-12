package com.dev.napolme.rateLimit;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private long perSecond = 10;
    private long burst = 20;
    private int defaultWeight = 1;
    private int cooldownSeconds = 30;
    private Map<String, Integer> endpointWeights = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPerSecond() {
        return perSecond;
    }

    public void setPerSecond(long perSecond) {
        this.perSecond = perSecond;
    }

    public long getBurst() {
        return burst;
    }

    public void setBurst(long burst) {
        this.burst = burst;
    }

    public int getDefaultWeight() {
        return defaultWeight;
    }

    public void setDefaultWeight(int defaultWeight) {
        this.defaultWeight = defaultWeight;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public Map<String, Integer> getEndpointWeights() {
        return endpointWeights;
    }

    public void setEndpointWeights(Map<String, Integer> endpointWeights) {
        this.endpointWeights = endpointWeights;
    }
}
