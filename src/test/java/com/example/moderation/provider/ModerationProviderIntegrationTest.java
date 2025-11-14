package com.example.moderation.provider;

import com.example.moderation.provider.mock.MockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ModerationProvider implementations
 */
@SpringBootTest
class ModerationProviderIntegrationTest {

    @Autowired
    private ModerationProviderFactory providerFactory;

    @Test
    void testProviderFactoryInitialization() {
        assertNotNull(providerFactory);
        List<ModerationProvider> providers = providerFactory.getAllProviders();
        assertFalse(providers.isEmpty());

        // Should have at least MockProvider
        assertTrue(providerFactory.hasProvider("mock"));
    }

    @Test
    void testMockProviderCleanText() {
        ModerationProvider provider = providerFactory.getProvider("mock");

        ModerationResult result = provider.moderateText("Hello, this is a clean message", new HashMap<>());

        assertNotNull(result);
        assertEquals("mock", result.getProviderName());
        assertEquals("LOW", result.getRiskLevel());
        assertTrue(result.getConfidenceScore() > 0.8);
        assertNotNull(result.getLabels());
        assertTrue(result.getLabels().contains("CLEAN"));
    }

    @Test
    void testMockProviderSpamText() {
        ModerationProvider provider = providerFactory.getProvider("mock");

        ModerationResult result = provider.moderateText("SPAM! BUY NOW! Click here for FREE scam!", new HashMap<>());

        assertNotNull(result);
        assertEquals("mock", result.getProviderName());
        assertEquals("HIGH", result.getRiskLevel());
        assertTrue(result.getLabels().contains("SPAM"));
    }

    @Test
    void testMockProviderViolenceText() {
        ModerationProvider provider = providerFactory.getProvider("mock");

        ModerationResult result = provider.moderateText("I will kill and attack with weapon", new HashMap<>());

        assertNotNull(result);
        assertEquals("HIGH", result.getRiskLevel());
        assertTrue(result.getLabels().contains("VIOLENCE"));
    }

    @Test
    void testMockProviderSuspiciousText() {
        ModerationProvider provider = providerFactory.getProvider("mock");

        ModerationResult result = provider.moderateText("This is maybe suspicious content", new HashMap<>());

        assertNotNull(result);
        assertEquals("MEDIUM", result.getRiskLevel());
        assertTrue(result.getLabels().contains("SUSPICIOUS"));
    }

    @Test
    void testProviderHealthCheck() {
        ModerationProvider provider = providerFactory.getProvider("mock");
        assertTrue(provider.isHealthy());
    }

    @Test
    void testProviderConfig() {
        ModerationProvider provider = providerFactory.getProvider("mock");
        ProviderConfig config = provider.getConfig();

        assertNotNull(config);
        assertEquals("mock", config.getProviderName());
        assertNotNull(config.getTimeout());
    }

    @Test
    void testUnknownProviderThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            providerFactory.getProvider("unknown-provider");
        });
    }
}
