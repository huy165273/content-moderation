package com.example.moderation.controller;

import com.example.moderation.dto.ModerationRequest;
import com.example.moderation.dto.ModerationResponse;
import com.example.moderation.service.ContentModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller cho content moderation
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ModerationController {

    private final ContentModerationService moderationService;

    /**
     * Endpoint để moderate một text đơn lẻ
     */
    @PostMapping("/moderate")
    public ResponseEntity<ModerationResponse> moderate(@Valid @RequestBody ModerationRequest request) {
        log.info("Received moderation request for ID: {}", request.getId());
        ModerationResponse response = moderationService.moderateContent(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint để moderate batch requests (cho load testing)
     */
    @PostMapping("/moderate/batch")
    public ResponseEntity<List<ModerationResponse>> moderateBatch(
            @Valid @RequestBody List<ModerationRequest> requests,
            @RequestParam(defaultValue = "10") int concurrency) {

        log.info("Received batch moderation request: {} items, concurrency: {}", requests.size(), concurrency);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<CompletableFuture<ModerationResponse>> futures = new ArrayList<>();

        for (ModerationRequest request : requests) {
            CompletableFuture<ModerationResponse> future = CompletableFuture.supplyAsync(
                    () -> moderationService.moderateContent(request),
                    executor
            );
            futures.add(future);
        }

        // Wait for all to complete
        List<ModerationResponse> responses = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        executor.shutdown();

        log.info("Batch moderation completed: {} responses", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
