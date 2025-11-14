package com.example.moderation.provider.deepcleer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration cho DeepCleer API
 */
@Configuration
@ConfigurationProperties(prefix = "deepcleer.api")
@Data
public class DeepCleerConfig {

    private String accessKey;
    private String appId;
    private String baseUrl;
    private String textModerationEndpoint;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private Boolean enabled;

    private RetryConfig retry;
    private CircuitBreakerConfig circuitBreaker;

    @Data
    public static class RetryConfig {
        private Integer maxAttempts = 3;
        private Long backoffDelayMs = 1000L;
        private Double backoffMultiplier = 2.0;
    }

    @Data
    public static class CircuitBreakerConfig {
        private Boolean enabled = true;
        private Integer failureRateThreshold = 50;
        private Long waitDurationMs = 60000L;
        private Integer slidingWindowSize = 10;
    }
}
