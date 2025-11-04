package com.example.moderation.service;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationPlusRequest;
import com.aliyun.green20220302.models.TextModerationPlusResponse;
import com.example.moderation.config.AlibabaCloudConfig;
import com.example.moderation.dto.ModerationRequest;
import com.example.moderation.dto.ModerationResponse;
import com.example.moderation.entity.ModerationResult;
import com.example.moderation.repository.ModerationResultRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service xử lý content moderation
 * Hỗ trợ cả real API calls và mock mode
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentModerationService {

    private final Client alibabaClient;
    private final AlibabaCloudConfig config;
    private final ModerationResultRepository resultRepository;
    private final Gson gson;

    /**
     * Moderate content và lưu kết quả vào database
     */
    public ModerationResponse moderateContent(ModerationRequest request) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        long startTime = System.currentTimeMillis();
        String requestId = request.getId();

        try {
            log.debug("Processing moderation request: {}", requestId);

            ModerationResponse response;
            if (config.getMockMode() != null && config.getMockMode()) {
                response = callMockApi(request);
            } else {
                response = callRealApi(request);
            }

            long latency = System.currentTimeMillis() - startTime;
            response.setLatencyMs(latency);
            response.setRequestId(requestId);

            // Lưu kết quả vào database
            saveResult(request, response, latency, true, null);

            log.debug("Request {} completed in {}ms", requestId, latency);
            return response;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Error processing request {}: {}", requestId, e.getMessage(), e);

            ModerationResponse errorResponse = ModerationResponse.builder()
                    .requestId(requestId)
                    .success(false)
                    .latencyMs(latency)
                    .errorMessage(e.getMessage())
                    .build();

            saveResult(request, errorResponse, latency, false, e.getMessage());

            return errorResponse;
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * Gọi Alibaba Cloud Content Moderation API thực tế
     */
    private ModerationResponse callRealApi(ModerationRequest request) throws Exception {
        // Tạo service parameters
        Map<String, String> serviceParams = new HashMap<>();
        serviceParams.put("content", request.getText());

        // Tạo request theo document Alibaba
        TextModerationPlusRequest apiRequest = new TextModerationPlusRequest()
                .setService(config.getServiceName())
                .setServiceParameters(gson.toJson(serviceParams));

        // Gọi API
        TextModerationPlusResponse apiResponse = alibabaClient.textModerationPlus(apiRequest);

        // Parse response
        return parseApiResponse(apiResponse);
    }

    /**
     * Mock API call cho testing
     */
    private ModerationResponse callMockApi(ModerationRequest request) {
        // Simulate API latency
        try {
            Thread.sleep((long) (Math.random() * 100 + 50)); // 50-150ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate mock response
        String text = request.getText().toLowerCase();
        String riskLevel = "LOW";
        double confidence = 0.95;

        // Simple keyword detection for demo
        if (text.contains("spam") || text.contains("bad") || text.contains("illegal")) {
            riskLevel = "HIGH";
            confidence = 0.85;
        } else if (text.contains("maybe") || text.contains("suspicious")) {
            riskLevel = "MEDIUM";
            confidence = 0.65;
        }

        Map<String, Object> mockData = new HashMap<>();
        mockData.put("riskLevel", riskLevel);
        mockData.put("confidence", confidence);
        mockData.put("labels", new String[]{"text_detection"});

        return ModerationResponse.builder()
                .riskLevel(riskLevel)
                .confidenceScore(confidence)
                .rawResponse(gson.toJson(mockData))
                .success(true)
                .build();
    }

    /**
     * Parse Alibaba API response
     */
    private ModerationResponse parseApiResponse(TextModerationPlusResponse apiResponse) {
        if (apiResponse.getStatusCode() == 200 && apiResponse.getBody().getCode() == 200) {
            String responseJson = gson.toJson(apiResponse.getBody().getData());

            // Extract risk level và confidence từ response
            // Note: Cấu trúc response thực tế cần xem từ API documentation
            String riskLevel = "LOW"; // Default
            Double confidence = 0.95;

            return ModerationResponse.builder()
                    .riskLevel(riskLevel)
                    .confidenceScore(confidence)
                    .rawResponse(responseJson)
                    .success(true)
                    .build();
        } else {
            throw new RuntimeException("API returned error: " + apiResponse.getBody().getMessage());
        }
    }

    /**
     * Lưu kết quả vào database
     */
    private void saveResult(ModerationRequest request, ModerationResponse response,
                            long latency, boolean success, String errorMessage) {
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
                .build();

        resultRepository.save(result);
    }
}
