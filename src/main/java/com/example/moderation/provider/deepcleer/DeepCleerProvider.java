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
    private final Gson requestGson;  // For serializing requests (no nulls)
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public DeepCleerProvider(DeepCleerConfig config, WebClient.Builder webClientBuilder,
                            Gson gson, Gson deepcleerGson) {
        this.config = config;
        this.gson = gson;  // For parsing responses
        this.requestGson = deepcleerGson;  // For serializing requests (no nulls)

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
     * Build DeepCleer API request according to official documentation
     * Note: tokenId is required by the API
     */
    private DeepCleerRequest buildRequest(String text, Map<String, Object> options) {
        // Extract parameters from options
        // tokenId is REQUIRED by DeepCleer API
        String tokenId = options != null ? (String) options.getOrDefault("userId", "anonymous_user") : "anonymous_user";
        String ip = options != null ? (String) options.get("ip") : null;
        String deviceId = options != null ? (String) options.get("deviceId") : null;
        String nickname = options != null ? (String) options.get("nickname") : null;

        // Build extra metadata if provided
        Map<String, Object> extra = null;
        if (options != null && options.containsKey("extra")) {
            extra = (Map<String, Object>) options.get("extra");
        }

        DeepCleerRequest.DataField data = DeepCleerRequest.DataField.builder()
                .text(text)
                .tokenId(tokenId)  // Required field
                .ip(ip)
                .deviceId(deviceId)
                .nickname(nickname)
                .extra(extra)
                .build();

        // Type should be TEXTRISK according to documentation
        String type = options != null ? (String) options.getOrDefault("type", "TEXTRISK") : "TEXTRISK";

        return DeepCleerRequest.builder()
                .accessKey(config.getAccessKey())
                .appId(config.getAppId())
                .eventId(config.getEventId())
                .type(type)
                .data(data)
                .build();
    }

    /**
     * Call DeepCleer API
     * Note: DeepCleer API returns Content-Type: text/plain but body is JSON
     */
    private DeepCleerResponse callApi(DeepCleerRequest request) {
        try {
            // Serialize request using requestGson (which omits null values)
            String requestBody = requestGson.toJson(request);
            log.info("=== DeepCleer API Call ===");
            log.info("Request URL: {}{}", config.getBaseUrl(), config.getTextModerationEndpoint());
            log.info("Request Body: {}", requestBody);
            log.info("Request Body Length: {} bytes", requestBody.getBytes().length);
            log.info("Timeout: {}ms", config.getReadTimeoutMs());

            // DeepCleer API returns Content-Type: text/plain but the body is actually JSON
            // So we need to retrieve as String first, then parse manually
            long apiStartTime = System.currentTimeMillis();
            String responseBody = webClient.post()
                    .uri(config.getTextModerationEndpoint())
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                            clientResponse -> {
                                int statusCode = clientResponse.statusCode().value();
                                log.error("HTTP Error Status: {}", statusCode);
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            log.error("HTTP Error Body: {}", body);
                                            return Mono.error(new ModerationException("API Error [" + statusCode + "]: " + body));
                                        });
                            })
                    .bodyToMono(String.class)
                    .block(Duration.ofMillis(config.getReadTimeoutMs() + 1000)); // Add 1s buffer to WebClient timeout

            long apiLatency = System.currentTimeMillis() - apiStartTime;
            log.info("API call completed in {}ms", apiLatency);

            if (responseBody == null || responseBody.isEmpty()) {
                throw new ModerationException("DeepCleer API returned null or empty response");
            }

            log.debug("DeepCleer API response body: {}", responseBody);

            // Parse JSON manually since response is text/plain
            DeepCleerResponse response = gson.fromJson(responseBody, DeepCleerResponse.class);

            if (response == null) {
                throw new ModerationException("Failed to parse DeepCleer response: " + responseBody);
            }

            return response;

        } catch (Exception e) {
            throw new ModerationException("Failed to call DeepCleer API", e);
        }
    }

    /**
     * Parse DeepCleer response to standard ModerationResult
     * According to official documentation
     */
    private ModerationResult parseResponse(DeepCleerResponse response, long latency) {
        if (response.getCode() != 1100) {
            throw new ModerationException("deepcleer", response.getCode(),
                    "DeepCleer API returned error: " + response.getMessage());
        }

        // Map DeepCleer risk levels to standard levels
        String riskLevel = mapRiskLevel(response.getRiskLevel());

        // Extract labels from the response
        List<String> labels = new ArrayList<>();
        labels.add(response.getRiskLabel1());
        if (response.getRiskLabel2() != null && !response.getRiskLabel2().isEmpty()) {
            labels.add(response.getRiskLabel2());
        }
        if (response.getRiskLabel3() != null && !response.getRiskLabel3().isEmpty()) {
            labels.add(response.getRiskLabel3());
        }

        // Calculate confidence score from allLabels
        Double confidenceScore = null;
        if (response.getAllLabels() != null && !response.getAllLabels().isEmpty()) {
            // Use the highest probability from all labels
            confidenceScore = response.getAllLabels().stream()
                    .map(DeepCleerResponse.AllLabel::getProbability)
                    .max(Double::compareTo)
                    .orElse(null);
        }

        // Build details map with comprehensive information
        Map<String, Object> details = new HashMap<>();
        details.put("code", response.getCode());
        details.put("requestId", response.getRequestId());
        details.put("originalRiskLevel", response.getRiskLevel());
        details.put("riskDescription", response.getRiskDescription());
        details.put("riskLabel1", response.getRiskLabel1());
        details.put("riskLabel2", response.getRiskLabel2());
        details.put("riskLabel3", response.getRiskLabel3());
        details.put("finalResult", response.getFinalResult());
        details.put("resultType", response.getResultType());

        // Add allLabels information
        if (response.getAllLabels() != null) {
            details.put("allLabelsCount", response.getAllLabels().size());
            details.put("allLabels", response.getAllLabels());
        }

        // Add auxiliary information if available
        if (response.getAuxInfo() != null) {
            details.put("auxInfo", response.getAuxInfo());
        }

        // Add risk detail if available
        if (response.getRiskDetail() != null) {
            details.put("riskDetail", response.getRiskDetail());
        }

        return ModerationResult.builder()
                .providerName("deepcleer")
                .riskLevel(riskLevel)
                .confidenceScore(confidenceScore)
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
