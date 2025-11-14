package com.example.moderation.provider.mock;

import com.example.moderation.provider.ModerationException;
import com.example.moderation.provider.ModerationProvider;
import com.example.moderation.provider.ModerationResult;
import com.example.moderation.provider.ProviderConfig;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Mock Provider cho testing
 * Không cần API credentials, simulate API behavior
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MockProvider implements ModerationProvider {

    private final Gson gson;

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public ModerationResult moderateText(String text, Map<String, Object> options) throws ModerationException {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Mock provider processing text length: {}", text.length());

            // Simulate API latency
            Thread.sleep((long) (Math.random() * 100 + 50)); // 50-150ms

            // Generate mock response based on content
            String lowerText = text.toLowerCase();
            String riskLevel = "LOW";
            double confidence = 0.95;
            List<String> labels = new ArrayList<>();

            // Simple keyword detection for demo
            if (containsAny(lowerText, "spam", "scam", "fraud", "phishing")) {
                riskLevel = "HIGH";
                confidence = 0.90;
                labels.add("SPAM");
            } else if (containsAny(lowerText, "violence", "kill", "attack", "weapon")) {
                riskLevel = "HIGH";
                confidence = 0.88;
                labels.add("VIOLENCE");
            } else if (containsAny(lowerText, "sex", "porn", "xxx", "adult")) {
                riskLevel = "HIGH";
                confidence = 0.92;
                labels.add("SEXUAL");
            } else if (containsAny(lowerText, "illegal", "drug", "smuggle")) {
                riskLevel = "HIGH";
                confidence = 0.87;
                labels.add("ILLEGAL");
            } else if (containsAny(lowerText, "maybe", "suspicious", "unclear")) {
                riskLevel = "MEDIUM";
                confidence = 0.65;
                labels.add("SUSPICIOUS");
            } else {
                labels.add("CLEAN");
            }

            long latency = System.currentTimeMillis() - startTime;

            Map<String, Object> mockData = new HashMap<>();
            mockData.put("riskLevel", riskLevel);
            mockData.put("confidence", confidence);
            mockData.put("labels", labels);
            mockData.put("mockMode", true);

            log.debug("Mock provider completed in {}ms, riskLevel: {}", latency, riskLevel);

            return ModerationResult.builder()
                    .providerName("mock")
                    .riskLevel(riskLevel)
                    .confidenceScore(confidence)
                    .labels(labels)
                    .details(mockData)
                    .rawResponse(gson.toJson(mockData))
                    .latencyMs(latency)
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModerationException("mock", null, "Mock provider interrupted", e);
        }
    }

    @Override
    public boolean isHealthy() {
        return true; // Mock provider is always healthy
    }

    @Override
    public ProviderConfig getConfig() {
        return ProviderConfig.builder()
                .providerName("mock")
                .baseUrl("mock://localhost")
                .timeout(5000)
                .enabled(true)
                .build();
    }

    /**
     * Helper method to check if text contains any of the keywords
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
