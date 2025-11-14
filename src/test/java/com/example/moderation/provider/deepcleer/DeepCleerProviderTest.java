package com.example.moderation.provider.deepcleer;

import com.example.moderation.provider.ModerationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DeepCleerProvider
 *
 * Make sure to set the following environment variables before running:
 * - DEEPCLEER_ACCESS_KEY: Your DeepCleer access key
 * - DEEPCLEER_APP_ID: Your DeepCleer app ID
 */
@SpringBootTest
public class DeepCleerProviderTest {

    @Autowired(required = false)
    private DeepCleerProvider deepCleerProvider;

    @Test
    public void testProviderAvailable() {
        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available (disabled in config)");
            System.out.println("Set DEEPCLEER_ENABLED=true to enable");
            return;
        }

        assertNotNull(deepCleerProvider, "DeepCleerProvider should be available");
        assertEquals("deepcleer", deepCleerProvider.getProviderName());
    }

    @Test
    public void testHealthCheck() {
        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available - skipping health check");
            return;
        }

        boolean healthy = deepCleerProvider.isHealthy();
        System.out.println("Provider health status: " + (healthy ? "✓ Healthy" : "✗ Unhealthy"));
        assertTrue(healthy, "Provider should be healthy initially");
    }

    @Test
    public void testModerateCleanText() {
        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available - skipping moderation test");
            return;
        }

        // Test with clean text (should return PASS)
        String cleanText = "Hello, this is a normal message.";
        Map<String, Object> options = new HashMap<>();
        options.put("userId", "test_user_001");
        options.put("ip", "118.89.214.89");

        System.out.println("\n=== Testing Clean Text ===");
        System.out.println("Text: " + cleanText);

        ModerationResult result = deepCleerProvider.moderateText(cleanText, options);

        assertNotNull(result);
        assertEquals("deepcleer", result.getProviderName());
        assertNotNull(result.getRiskLevel());
        assertNotNull(result.getRawResponse());

        System.out.println("✓ Risk Level: " + result.getRiskLevel());
        System.out.println("✓ Labels: " + result.getLabels());
        System.out.println("✓ Latency: " + result.getLatencyMs() + "ms");
        System.out.println("✓ Confidence: " + result.getConfidenceScore());
        System.out.println("✓ Raw Response: " + result.getRawResponse());
    }

    @Test
    public void testModerateRiskyText() {
        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available - skipping moderation test");
            return;
        }

        // Test with risky text from documentation example
        String riskyText = "Add me on QQ: qq12345";
        Map<String, Object> options = new HashMap<>();
        options.put("userId", "test_user_002");
        options.put("ip", "118.89.214.89");
        options.put("deviceId", "device_123");
        options.put("nickname", "TestUser");

        System.out.println("\n=== Testing Risky Text ===");
        System.out.println("Text: " + riskyText);

        ModerationResult result = deepCleerProvider.moderateText(riskyText, options);

        assertNotNull(result);
        assertEquals("deepcleer", result.getProviderName());
        assertNotNull(result.getRiskLevel());

        System.out.println("✓ Risk Level: " + result.getRiskLevel());
        System.out.println("✓ Labels: " + result.getLabels());
        System.out.println("✓ Latency: " + result.getLatencyMs() + "ms");
        System.out.println("✓ Confidence: " + result.getConfidenceScore());

        // Print details
        if (result.getDetails() != null) {
            System.out.println("✓ Details:");
            result.getDetails().forEach((key, value) ->
                System.out.println("  - " + key + ": " + value)
            );
        }

        System.out.println("✓ Raw Response: " + result.getRawResponse());

        // Should detect as risky (MEDIUM or HIGH)
        assertTrue(
            "MEDIUM".equals(result.getRiskLevel()) || "HIGH".equals(result.getRiskLevel()),
            "Risky text should be flagged as MEDIUM or HIGH"
        );
    }

    @Test
    public void testModerateWithExtraMetadata() {
        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available - skipping moderation test");
            return;
        }

        String text = "Test message with metadata";

        // Create extra metadata
        Map<String, Object> extra = new HashMap<>();
        extra.put("topic", "12345");
        extra.put("atId", "username1");
        extra.put("room", "ceshi123");
        extra.put("receiveTokenId", "username2");

        Map<String, Object> options = new HashMap<>();
        options.put("userId", "test_user_003");
        options.put("ip", "192.168.1.1");
        options.put("deviceId", "device_456");
        options.put("nickname", "TestNickname");
        options.put("extra", extra);

        System.out.println("\n=== Testing With Extra Metadata ===");
        System.out.println("Text: " + text);
        System.out.println("Extra metadata: " + extra);

        ModerationResult result = deepCleerProvider.moderateText(text, options);

        assertNotNull(result);
        System.out.println("✓ Risk Level: " + result.getRiskLevel());
        System.out.println("✓ Latency: " + result.getLatencyMs() + "ms");
        System.out.println("✓ Request completed successfully with extra metadata");
    }

    @Test
    public void testConfiguration() {
        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available - skipping config test");
            return;
        }

        var config = deepCleerProvider.getConfig();

        System.out.println("\n=== Provider Configuration ===");
        System.out.println("Provider Name: " + config.getProviderName());
        System.out.println("Base URL: " + config.getBaseUrl());
        System.out.println("Timeout: " + config.getTimeout() + "ms");
        System.out.println("Enabled: " + config.getEnabled());

        assertNotNull(config);
        assertEquals("deepcleer", config.getProviderName());
        assertTrue(config.getEnabled());
    }
}
