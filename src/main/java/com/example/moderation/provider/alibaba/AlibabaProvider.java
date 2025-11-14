package com.example.moderation.provider.alibaba;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationPlusRequest;
import com.aliyun.green20220302.models.TextModerationPlusResponse;
import com.example.moderation.config.AlibabaCloudConfig;
import com.example.moderation.provider.ModerationException;
import com.example.moderation.provider.ModerationProvider;
import com.example.moderation.provider.ModerationResult;
import com.example.moderation.provider.ProviderConfig;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Alibaba Cloud Content Moderation Provider
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "alibaba.cloud", name = "mock-mode", havingValue = "false")
public class AlibabaProvider implements ModerationProvider {

    private final Client alibabaClient;
    private final AlibabaCloudConfig config;
    private final Gson gson;

    @Override
    public String getProviderName() {
        return "alibaba";
    }

    @Override
    public ModerationResult moderateText(String text, Map<String, Object> options) throws ModerationException {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Calling Alibaba Cloud API for text length: {}", text.length());

            // Tạo service parameters
            Map<String, String> serviceParams = new HashMap<>();
            serviceParams.put("content", text);

            // Tạo request theo document Alibaba
            TextModerationPlusRequest apiRequest = new TextModerationPlusRequest()
                    .setService(config.getServiceName())
                    .setServiceParameters(gson.toJson(serviceParams));

            // Gọi API
            TextModerationPlusResponse apiResponse = alibabaClient.textModerationPlus(apiRequest);

            long latency = System.currentTimeMillis() - startTime;

            log.debug("Alibaba API call completed in {}ms", latency);

            // Parse response
            return parseApiResponse(apiResponse, latency);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Alibaba API call failed after {}ms: {}", latency, e.getMessage(), e);
            throw new ModerationException("alibaba", null, "Alibaba API failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        return alibabaClient != null;
    }

    @Override
    public ProviderConfig getConfig() {
        return ProviderConfig.builder()
                .providerName("alibaba")
                .baseUrl(config.getEndpoint())
                .timeout(config.getReadTimeout())
                .enabled(true)
                .build();
    }

    /**
     * Parse Alibaba API response
     */
    private ModerationResult parseApiResponse(TextModerationPlusResponse apiResponse, long latency) {
        if (apiResponse.getStatusCode() == 200 && apiResponse.getBody().getCode() == 200) {
            String responseJson = gson.toJson(apiResponse.getBody().getData());

            // Extract risk level và confidence từ response
            // Note: Cấu trúc response thực tế cần xem từ API documentation
            String riskLevel = "LOW"; // Default
            Double confidence = 0.95;

            return ModerationResult.builder()
                    .providerName("alibaba")
                    .riskLevel(riskLevel)
                    .confidenceScore(confidence)
                    .rawResponse(responseJson)
                    .latencyMs(latency)
                    .build();
        } else {
            throw new ModerationException("alibaba", apiResponse.getBody().getCode(),
                    "API returned error: " + apiResponse.getBody().getMessage());
        }
    }
}
