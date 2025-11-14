package com.example.moderation.controller;

import com.example.moderation.dto.ModerationRequest;
import com.example.moderation.dto.ModerationResponse;
import com.example.moderation.repository.ModerationResultRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for ModerationController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "content-moderation.active-provider=mock"
})
class ModerationControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ModerationResultRepository resultRepository;

    @AfterEach
    void cleanup() {
        resultRepository.deleteAll();
    }

    @Test
    void testHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/health", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
    }

    @Test
    void testModerateEndpoint() {
        ModerationRequest request = ModerationRequest.builder()
                .id(UUID.randomUUID().toString())
                .text("This is a test message")
                .build();

        ResponseEntity<ModerationResponse> response = restTemplate.postForEntity(
                "/api/v1/moderate",
                request,
                ModerationResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        assertNotNull(response.getBody().getRiskLevel());
        assertEquals(request.getId(), response.getBody().getRequestId());
    }

    @Test
    void testModerateSpamContent() {
        ModerationRequest request = ModerationRequest.builder()
                .id(UUID.randomUUID().toString())
                .text("SPAM! BUY NOW! Click here!")
                .build();

        ResponseEntity<ModerationResponse> response = restTemplate.postForEntity(
                "/api/v1/moderate",
                request,
                ModerationResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("HIGH", response.getBody().getRiskLevel());
    }

    @Test
    void testModerateBatchEndpoint() {
        List<ModerationRequest> requests = List.of(
                ModerationRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .text("Clean message 1")
                        .build(),
                ModerationRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .text("SPAM message 2")
                        .build(),
                ModerationRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .text("Clean message 3")
                        .build()
        );

        ResponseEntity<List> response = restTemplate.postForEntity(
                "/api/v1/moderate/batch?concurrency=2",
                requests,
                List.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
    }

    @Test
    void testModerateWithInvalidRequest() {
        // Request with missing ID
        ModerationRequest request = ModerationRequest.builder()
                .text("Test message")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/moderate",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testModerateWithEmptyText() {
        // Request with empty text
        ModerationRequest request = ModerationRequest.builder()
                .id(UUID.randomUUID().toString())
                .text("")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/moderate",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testMultipleRequestsSequentially() {
        for (int i = 0; i < 5; i++) {
            ModerationRequest request = ModerationRequest.builder()
                    .id(UUID.randomUUID().toString())
                    .text("Test message " + i)
                    .build();

            ResponseEntity<ModerationResponse> response = restTemplate.postForEntity(
                    "/api/v1/moderate",
                    request,
                    ModerationResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().getSuccess());
        }

        // Verify all saved
        assertEquals(5, resultRepository.count());
    }
}
