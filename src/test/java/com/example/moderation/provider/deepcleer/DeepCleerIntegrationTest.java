package com.example.moderation.provider.deepcleer;

import com.example.moderation.provider.ModerationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for DeepCleerProvider with detailed logging
 * This test matches the exact curl command provided by the user
 */
@SpringBootTest
public class DeepCleerIntegrationTest {

    @Autowired(required = false)
    private DeepCleerProvider deepCleerProvider;

    @Autowired(required = false)
    private DeepCleerConfig config;

    @Test
    public void testConfigurationLoaded() {
        System.out.println("\n========================================");
        System.out.println("=== DeepCleer Configuration Test ===");
        System.out.println("========================================");

        if (config == null) {
            System.out.println("❌ DeepCleerConfig is NULL - check application.yml");
            fail("DeepCleerConfig should be loaded");
        }

        System.out.println("✓ Configuration loaded successfully");
        System.out.println("  - Access Key: " + maskKey(config.getAccessKey()));
        System.out.println("  - App ID: " + config.getAppId());
        System.out.println("  - Event ID: " + config.getEventId());
        System.out.println("  - Base URL: " + config.getBaseUrl());
        System.out.println("  - Endpoint: " + config.getTextModerationEndpoint());
        System.out.println("  - Full URL: " + config.getBaseUrl() + config.getTextModerationEndpoint());
        System.out.println("  - Connect Timeout: " + config.getConnectTimeoutMs() + "ms");
        System.out.println("  - Read Timeout: " + config.getReadTimeoutMs() + "ms");
        System.out.println("  - Enabled: " + config.getEnabled());

        // Validate configuration
        assertNotNull(config.getAccessKey(), "Access key should not be null");
        assertNotNull(config.getAppId(), "App ID should not be null");
        assertNotNull(config.getEventId(), "Event ID should not be null");
        assertNotNull(config.getBaseUrl(), "Base URL should not be null");
        assertNotNull(config.getTextModerationEndpoint(), "Endpoint should not be null");
        assertEquals("/text/v4", config.getTextModerationEndpoint(), "Endpoint should be /text/v4");
    }

    @Test
    public void testProviderInitialized() {
        System.out.println("\n========================================");
        System.out.println("=== DeepCleer Provider Initialization ===");
        System.out.println("========================================");

        if (deepCleerProvider == null) {
            System.out.println("❌ DeepCleerProvider is NULL");
            System.out.println("Possible reasons:");
            System.out.println("  1. DEEPCLEER_ENABLED is not set to true");
            System.out.println("  2. Configuration property 'deepcleer.api.enabled' is false");
            System.out.println("  3. Bean initialization failed");
            fail("DeepCleerProvider should be initialized");
        }

        System.out.println("✓ Provider initialized successfully");
        System.out.println("  - Provider Name: " + deepCleerProvider.getProviderName());
        System.out.println("  - Health Status: " + (deepCleerProvider.isHealthy() ? "Healthy" : "Unhealthy"));

        assertEquals("deepcleer", deepCleerProvider.getProviderName());
        assertTrue(deepCleerProvider.isHealthy(), "Provider should be healthy initially");
    }

    /**
     * Test with the exact text from the user's curl command
     * curl --location 'http://localhost:8080/api/v1/moderate' \
     * --header 'Content-Type: application/json' \
     * --data '{
     *     "id": "6",
     *     "text": "Đồ ngu, mày làm gì cũng sai.",
     *     "runId": "test-run-1"
     * }'
     */
    @Test
    public void testModerateVietnameseText() {
        System.out.println("\n========================================");
        System.out.println("=== Testing Vietnamese Text (User's Example) ===");
        System.out.println("========================================");

        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available - skipping test");
            return;
        }

        // Exact text from user's curl command
        String text = "Đồ ngu, mày làm gì cũng sai.";

        Map<String, Object> options = new HashMap<>();
        options.put("userId", "6"); // Using ID from curl

        System.out.println("Request:");
        System.out.println("  - Text: " + text);
        System.out.println("  - Text Length: " + text.length());
        System.out.println("  - User ID: " + options.get("userId"));

        try {
            long startTime = System.currentTimeMillis();
            ModerationResult result = deepCleerProvider.moderateText(text, options);
            long duration = System.currentTimeMillis() - startTime;

            System.out.println("\n✓ API Call Successful!");
            System.out.println("  - Duration: " + duration + "ms");
            System.out.println("\nResponse:");
            System.out.println("  - Provider: " + result.getProviderName());
            System.out.println("  - Risk Level: " + result.getRiskLevel());
            System.out.println("  - Confidence Score: " + result.getConfidenceScore());
            System.out.println("  - Labels: " + result.getLabels());
            System.out.println("  - Latency: " + result.getLatencyMs() + "ms");

            if (result.getDetails() != null) {
                System.out.println("\nDetails:");
                result.getDetails().forEach((key, value) -> {
                    if (!"rawResponse".equals(key)) { // Skip raw response for readability
                        System.out.println("  - " + key + ": " + value);
                    }
                });
            }

            System.out.println("\nRaw Response:");
            System.out.println(result.getRawResponse());

            // Assertions
            assertNotNull(result, "Result should not be null");
            assertEquals("deepcleer", result.getProviderName());
            assertNotNull(result.getRiskLevel(), "Risk level should not be null");
            assertNotNull(result.getLabels(), "Labels should not be null");
            assertTrue(result.getLatencyMs() > 0, "Latency should be positive");

            System.out.println("\n✓ All assertions passed!");

        } catch (Exception e) {
            System.err.println("\n❌ API Call Failed!");
            System.err.println("Error Type: " + e.getClass().getSimpleName());
            System.err.println("Error Message: " + e.getMessage());

            if (e.getCause() != null) {
                System.err.println("Caused By: " + e.getCause().getClass().getSimpleName());
                System.err.println("Cause Message: " + e.getCause().getMessage());
            }

            System.err.println("\nStack Trace:");
            e.printStackTrace();

            fail("Moderation API call failed: " + e.getMessage());
        }
    }

    /**
     * Test with the exact text from the direct DeepCleer API curl
     */
    @Test
    public void testModerateDirectApiExample() {
        System.out.println("\n========================================");
        System.out.println("=== Testing Direct API Example Text ===");
        System.out.println("========================================");

        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available - skipping test");
            return;
        }

        // Text from direct API curl: "Đmm "
        String text = "Đmm ";

        Map<String, Object> options = new HashMap<>();
        options.put("userId", "4567898765jhgfdsa"); // tokenId from curl

        System.out.println("Request:");
        System.out.println("  - Text: '" + text + "'");
        System.out.println("  - Text Length: " + text.length());
        System.out.println("  - Token ID: " + options.get("userId"));

        try {
            long startTime = System.currentTimeMillis();
            ModerationResult result = deepCleerProvider.moderateText(text, options);
            long duration = System.currentTimeMillis() - startTime;

            System.out.println("\n✓ API Call Successful!");
            System.out.println("  - Duration: " + duration + "ms");
            System.out.println("\nResponse:");
            System.out.println("  - Provider: " + result.getProviderName());
            System.out.println("  - Risk Level: " + result.getRiskLevel());
            System.out.println("  - Confidence Score: " + result.getConfidenceScore());
            System.out.println("  - Labels: " + result.getLabels());
            System.out.println("  - Latency: " + result.getLatencyMs() + "ms");

            if (result.getDetails() != null) {
                System.out.println("\nDetails:");
                result.getDetails().forEach((key, value) -> {
                    if (!"rawResponse".equals(key)) {
                        System.out.println("  - " + key + ": " + value);
                    }
                });
            }

            System.out.println("\nRaw Response:");
            System.out.println(result.getRawResponse());

            // Assertions
            assertNotNull(result);
            assertEquals("deepcleer", result.getProviderName());
            assertNotNull(result.getRiskLevel());

            System.out.println("\n✓ All assertions passed!");

        } catch (Exception e) {
            System.err.println("\n❌ API Call Failed!");
            System.err.println("Error Type: " + e.getClass().getSimpleName());
            System.err.println("Error Message: " + e.getMessage());

            if (e.getCause() != null) {
                System.err.println("Caused By: " + e.getCause().getClass().getSimpleName());
                System.err.println("Cause Message: " + e.getCause().getMessage());
            }

            System.err.println("\nStack Trace:");
            e.printStackTrace();

            fail("Moderation API call failed: " + e.getMessage());
        }
    }

    /**
     * Test with various Vietnamese offensive texts
     */
    @Test
    public void testVariousVietnameseTexts() {
        System.out.println("\n========================================");
        System.out.println("=== Testing Various Vietnamese Texts ===");
        System.out.println("========================================");

        if (deepCleerProvider == null) {
            System.out.println("⚠️ DeepCleerProvider is not available - skipping test");
            return;
        }

        String[] testTexts = {
            "Xin chào, tôi là người dùng mới",  // Clean text
            "Đồ ngu, mày làm gì cũng sai.",      // Offensive (user's example)
            "Đmm ",                              // Offensive (direct API example)
            "Tôi yêu Việt Nam"                   // Clean text
        };

        for (int i = 0; i < testTexts.length; i++) {
            String text = testTexts[i];
            System.out.println("\n--- Test Case " + (i + 1) + " ---");
            System.out.println("Text: '" + text + "'");

            try {
                Map<String, Object> options = new HashMap<>();
                options.put("userId", "test_user_" + i);

                ModerationResult result = deepCleerProvider.moderateText(text, options);

                System.out.println("✓ Success - Risk Level: " + result.getRiskLevel() +
                                 ", Labels: " + result.getLabels() +
                                 ", Latency: " + result.getLatencyMs() + "ms");

                assertNotNull(result);
                assertNotNull(result.getRiskLevel());

            } catch (Exception e) {
                System.err.println("❌ Failed: " + e.getMessage());
                fail("Test case " + (i + 1) + " failed: " + e.getMessage());
            }
        }

        System.out.println("\n✓ All test cases completed successfully!");
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
