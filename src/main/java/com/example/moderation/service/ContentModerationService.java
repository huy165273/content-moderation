package com.example.moderation.service;

import com.example.moderation.dto.ModerationRequest;
import com.example.moderation.dto.ModerationResponse;
import com.example.moderation.entity.ModerationResult;
import com.example.moderation.exception.DuplicateRequestIdException;
import com.example.moderation.provider.ModerationProvider;
import com.example.moderation.provider.ModerationProviderFactory;
import com.example.moderation.repository.ModerationResultRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * Service xử lý content moderation với multi-provider support
 * Hỗ trợ DeepCleer, Alibaba, Mock providers
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentModerationService {

    private final ModerationProviderFactory providerFactory;
    private final ModerationResultRepository resultRepository;
    private final Gson gson;

    @Value("${content-moderation.active-provider:mock}")
    private String activeProviderName;

    @Value("${content-moderation.fallback.enabled:false}")
    private Boolean fallbackEnabled;

    @Value("${content-moderation.fallback.secondary-provider:mock}")
    private String secondaryProviderName;

    /**
     * Moderate content và lưu kết quả vào database
     */
    public ModerationResponse moderateContent(ModerationRequest request) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        long startTime = System.currentTimeMillis();
        String requestId = request.getId();

        try {
            log.debug("Processing moderation request: {} with provider: {}", requestId, activeProviderName);

            // Validate: Kiểm tra request ID đã tồn tại chưa
            validateRequestIdNotExists(requestId);

            // Get provider
            ModerationProvider provider = providerFactory.getProvider(activeProviderName);

            // Call provider with fallback support
            com.example.moderation.provider.ModerationResult providerResult;
            try {
                providerResult = provider.moderateText(request.getText(), new HashMap<>());
            } catch (Exception e) {
                log.error("Primary provider {} failed: {}", activeProviderName, e.getMessage());

                // Fallback to secondary provider if enabled
                if (fallbackEnabled && providerFactory.hasProvider(secondaryProviderName)) {
                    log.info("Falling back to secondary provider: {}", secondaryProviderName);
                    provider = providerFactory.getProvider(secondaryProviderName);
                    providerResult = provider.moderateText(request.getText(), new HashMap<>());
                } else {
                    throw e;
                }
            }

            long latency = System.currentTimeMillis() - startTime;

            // Convert to response DTO
            ModerationResponse response = ModerationResponse.builder()
                    .requestId(requestId)
                    .riskLevel(providerResult.getRiskLevel())
                    .confidenceScore(providerResult.getConfidenceScore())
                    .rawResponse(providerResult.getRawResponse())
                    .latencyMs(latency)
                    .success(true)
                    .build();

            // Lưu kết quả vào database
            saveResult(request, response, latency, true, null, provider.getProviderName());

            log.debug("Request {} completed in {}ms with provider {}", requestId, latency, provider.getProviderName());
            return response;

        } catch (DuplicateRequestIdException e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Duplicate request ID {}: {}", requestId, e.getMessage(), e);

            // Don't save to database again for duplicate requests
            ModerationResponse errorResponse = ModerationResponse.builder()
                    .requestId(requestId)
                    .success(false)
                    .latencyMs(latency)
                    .errorMessage(e.getMessage())
                    .build();

            return errorResponse;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Error processing request {}: {}", requestId, e.getMessage(), e);

            ModerationResponse errorResponse = ModerationResponse.builder()
                    .requestId(requestId)
                    .success(false)
                    .latencyMs(latency)
                    .errorMessage(e.getMessage())
                    .build();

            // Try to save error result (may fail if it's a database error)
            try {
                saveResult(request, errorResponse, latency, false, e.getMessage(), activeProviderName);
            } catch (Exception saveEx) {
                log.warn("Failed to save error result for request {}: {}", requestId, saveEx.getMessage());
            }

            return errorResponse;
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * Validate request ID chưa tồn tại trong database.
     * Throw DuplicateRequestIdException nếu đã tồn tại.
     *
     * @param requestId Request ID cần validate
     * @throws DuplicateRequestIdException nếu request ID đã tồn tại
     */
    private void validateRequestIdNotExists(String requestId) {
        if (resultRepository.existsByRequestId(requestId)) {
            log.warn("Duplicate request ID detected: {}", requestId);
            throw new DuplicateRequestIdException(requestId);
        }
    }

    /**
     * Lưu kết quả vào database
     */
    private void saveResult(ModerationRequest request, ModerationResponse response,
                            long latency, boolean success, String errorMessage, String providerName) {
        ModerationResult result = ModerationResult.builder()
                .requestId(request.getId())
                .runId(request.getRunId())
                .payload(gson.toJson(request))
                .responseBody(response.getRawResponse())
                .statusCode(success ? 200 : 500)
                .latencyMs(latency)
                .timestamp(LocalDateTime.now())
                .errorMessage(errorMessage)
                .attempts(1)
                .success(success)
                .riskLevel(response.getRiskLevel())
                .confidenceScore(response.getConfidenceScore())
                .providerName(providerName)
                .build();

        resultRepository.save(result);
    }
}
