package com.example.moderation.provider.deepcleer;

import com.example.moderation.provider.ModerationException;
import com.example.moderation.provider.ModerationProvider;
import com.example.moderation.provider.ModerationResult;
import com.example.moderation.provider.ProviderConfig;
import com.google.gson.Gson;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DeepCleer Content Moderation Provider
 * Tích hợp với DeepCleer AI Text Moderation API
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "deepcleer.api", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DeepCleerProvider implements ModerationProvider {

    private final DeepCleerConfig config;
    private final WebClient webClient;
    private final Gson gson;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public DeepCleerProvider(DeepCleerConfig config, WebClient.Builder webClientBuilder, Gson gson) {
        this.config = config;
        this.gson = gson;

        // Configure WebClient
        this.webClient = webClientBuilder
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();

        // Configure Circuit Breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(config.getCircuitBreaker().getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(config.getCircuitBreaker().getWaitDurationMs()))
                .slidingWindowSize(config.getCircuitBreaker().getSlidingWindowSize())
                .build();

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);
        this.circuitBreaker = cbRegistry.circuitBreaker("deepcleer");

        // Configure Retry
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(config.getRetry().getMaxAttempts())
                .waitDuration(Duration.ofMillis(config.getRetry().getBackoffDelayMs()))
                .build();

        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        this.retry = retryRegistry.retry("deepcleer");

        log.info("DeepCleerProvider initialized with baseUrl: {}", config.getBaseUrl());
    }

    @Override
    public String getProviderName() {
        return "deepcleer";
    }

    @Override
    public ModerationResult moderateText(String text, Map<String, Object> options) throws ModerationException {
        long startTime = System.currentTimeMillis();

        try {
            // Build request
            DeepCleerRequest request = buildRequest(text, options);

            log.debug("Calling DeepCleer API for text length: {}", text.length());

            // Call API with Circuit Breaker + Retry
            DeepCleerResponse response = CircuitBreaker.decorateSupplier(circuitBreaker,
                    () -> Retry.decorateSupplier(retry, () -> callApi(request)).get()
            ).get();

            long latency = System.currentTimeMillis() - startTime;

            log.debug("DeepCleer API call completed in {}ms, riskLevel: {}", latency, response.getRiskLevel());

            // Parse response
            return parseResponse(response, latency);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("DeepCleer API call failed after {}ms: {}", latency, e.getMessage(), e);
            throw new ModerationException("deepcleer", null, "DeepCleer API failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }

    @Override
    public ProviderConfig getConfig() {
        return ProviderConfig.builder()
                .providerName("deepcleer")
                .baseUrl(config.getBaseUrl())
                .timeout(config.getReadTimeoutMs())
                .enabled(config.getEnabled())
                .build();
    }

    /**
     * Build DeepCleer API request
     */
    private DeepCleerRequest buildRequest(String text, Map<String, Object> options) {
        String eventId = UUID.randomUUID().toString();

        DeepCleerRequest.DataField data = DeepCleerRequest.DataField.builder()
                .text(text)
                .tokenId(options.getOrDefault("userId", "anonymous").toString())
                .channel("TEXT")
                .build();

        return DeepCleerRequest.builder()
                .accessKey(config.getAccessKey())
                .appId(config.getAppId())
                .eventId(eventId)
                .type(options.getOrDefault("type", "ZHIBO").toString())
                .data(data)
                .build();
    }

    /**
     * Call DeepCleer API
     */
    private DeepCleerResponse callApi(DeepCleerRequest request) {
        try {
            DeepCleerResponse response = webClient.post()
                    .uri(config.getTextModerationEndpoint())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new ModerationException("API Error: " + body))))
                    .bodyToMono(DeepCleerResponse.class)
                    .timeout(Duration.ofMillis(config.getReadTimeoutMs()))
                    .block();

            if (response == null) {
                throw new ModerationException("DeepCleer API returned null response");
            }

            return response;

        } catch (Exception e) {
            throw new ModerationException("Failed to call DeepCleer API", e);
        }
    }

    /**
     * Parse DeepCleer response to standard ModerationResult
     */
    private ModerationResult parseResponse(DeepCleerResponse response, long latency) {
        if (response.getCode() != 1100) {
            throw new ModerationException("deepcleer", response.getCode(),
                    "DeepCleer API returned error: " + response.getMessage());
        }

        // Map DeepCleer risk levels to standard levels
        String riskLevel = mapRiskLevel(response.getRiskLevel());

        // Extract labels
        List<String> labels = new ArrayList<>();
        if (response.getDetail() != null && response.getDetail().getHits() != null) {
            labels = response.getDetail().getHits().stream()
                    .map(DeepCleerResponse.Hit::getLabel)
                    .collect(Collectors.toList());
        }

        // Build details map
        Map<String, Object> details = new HashMap<>();
        details.put("code", response.getCode());
        details.put("originalRiskLevel", response.getRiskLevel());
        if (response.getDetail() != null) {
            details.put("hitCount", response.getDetail().getHits() != null ? response.getDetail().getHits().size() : 0);
        }

        return ModerationResult.builder()
                .providerName("deepcleer")
                .riskLevel(riskLevel)
                .confidenceScore(response.getScore())
                .labels(labels)
                .details(details)
                .rawResponse(gson.toJson(response))
                .latencyMs(latency)
                .build();
    }

    /**
     * Map DeepCleer risk levels to standard levels
     * PASS -> LOW
     * REVIEW -> MEDIUM
     * REJECT -> HIGH
     */
    private String mapRiskLevel(String deepCleerLevel) {
        if (deepCleerLevel == null) {
            return "UNKNOWN";
        }

        return switch (deepCleerLevel.toUpperCase()) {
            case "PASS" -> "LOW";
            case "REVIEW" -> "MEDIUM";
            case "REJECT" -> "HIGH";
            default -> "UNKNOWN";
        };
    }
}
