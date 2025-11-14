package com.example.moderation.service;

import com.example.moderation.dto.ModerationRequest;
import com.example.moderation.dto.ModerationResponse;
import com.example.moderation.entity.ModerationResult;
import com.example.moderation.repository.ModerationResultRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ContentModerationService with provider strategy
 */
@SpringBootTest
@TestPropertySource(properties = {
        "content-moderation.active-provider=mock",
        "content-moderation.fallback.enabled=true",
        "content-moderation.fallback.secondary-provider=mock"
})
class ContentModerationServiceIntegrationTest {

    @Autowired
    private ContentModerationService moderationService;

    @Autowired
    private ModerationResultRepository resultRepository;

    @AfterEach
    void cleanup() {
        // Clean up test data after each test
        resultRepository.deleteAll();
    }

    @Test
    void testModerateCleanContent() {
        ModerationRequest request = ModerationRequest.builder()
                .id(UUID.randomUUID().toString())
                .text("Hello, this is a clean message")
                .runId("test-run-1")
                .build();

        ModerationResponse response = moderationService.moderateContent(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("LOW", response.getRiskLevel());
        assertNotNull(response.getLatencyMs());
        assertTrue(response.getLatencyMs() > 0);
        assertNull(response.getErrorMessage());

        // Verify saved to database
        assertTrue(resultRepository.existsByRequestId(request.getId()));
        ModerationResult savedResult = resultRepository.findByRequestId(request.getId()).orElseThrow();
        assertEquals("mock", savedResult.getProviderName());
        assertEquals("LOW", savedResult.getRiskLevel());
    }

    @Test
    void testModerateSpamContent() {
        ModerationRequest request = ModerationRequest.builder()
                .id(UUID.randomUUID().toString())
                .text("SPAM! BUY NOW! Free scam!")
                .runId("test-run-2")
                .build();

        ModerationResponse response = moderationService.moderateContent(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("HIGH", response.getRiskLevel());

        // Verify saved to database
        ModerationResult savedResult = resultRepository.findByRequestId(request.getId()).orElseThrow();
        assertEquals("HIGH", savedResult.getRiskLevel());
        assertEquals("mock", savedResult.getProviderName());
    }

    @Test
    void testModerateSuspiciousContent() {
        ModerationRequest request = ModerationRequest.builder()
                .id(UUID.randomUUID().toString())
                .text("This is maybe suspicious")
                .runId("test-run-3")
                .build();

        ModerationResponse response = moderationService.moderateContent(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("MEDIUM", response.getRiskLevel());
    }

    @Test
    void testDuplicateRequestIdThrowsException() {
        String requestId = UUID.randomUUID().toString();

        ModerationRequest request1 = ModerationRequest.builder()
                .id(requestId)
                .text("First request")
                .build();

        // First request should succeed
        ModerationResponse response1 = moderationService.moderateContent(request1);
        assertTrue(response1.getSuccess());

        // Clean up to avoid unique constraint violation in subsequent test runs
        // In production, we would handle this differently (e.g., using a proper exception)

        // Second request with same ID should fail with error response
        ModerationRequest request2 = ModerationRequest.builder()
                .id(requestId)
                .text("Second request")
                .build();

        ModerationResponse response2 = moderationService.moderateContent(request2);
        assertFalse(response2.getSuccess());
        assertNotNull(response2.getErrorMessage());
        assertTrue(response2.getErrorMessage().toLowerCase().contains("duplicate") ||
                   response2.getErrorMessage().toLowerCase().contains("already exists") ||
                   response2.getErrorMessage().toLowerCase().contains("tồn tại") ||
                   response2.getErrorMessage().toLowerCase().contains("đã tồn tại"));
    }

    @Test
    void testMultipleRequestsDifferentRiskLevels() {
        // Test batch of different content types
        String[] texts = {
                "Clean message",
                "SPAM scam fraud",
                "Maybe suspicious",
                "Violence kill attack"
        };

        String[] expectedRiskLevels = {"LOW", "HIGH", "MEDIUM", "HIGH"};

        for (int i = 0; i < texts.length; i++) {
            ModerationRequest request = ModerationRequest.builder()
                    .id(UUID.randomUUID().toString())
                    .text(texts[i])
                    .runId("batch-test")
                    .build();

            ModerationResponse response = moderationService.moderateContent(request);

            assertTrue(response.getSuccess());
            assertEquals(expectedRiskLevels[i], response.getRiskLevel(),
                    "Failed for text: " + texts[i]);
        }

        // Verify all saved
        assertEquals(texts.length, resultRepository.count());
    }

    @Test
    void testResponseContainsAllRequiredFields() {
        ModerationRequest request = ModerationRequest.builder()
                .id(UUID.randomUUID().toString())
                .text("Test message")
                .runId("field-test")
                .build();

        ModerationResponse response = moderationService.moderateContent(request);

        assertNotNull(response.getRequestId());
        assertNotNull(response.getRiskLevel());
        assertNotNull(response.getConfidenceScore());
        assertNotNull(response.getRawResponse());
        assertNotNull(response.getLatencyMs());
        assertNotNull(response.getSuccess());
    }
}
