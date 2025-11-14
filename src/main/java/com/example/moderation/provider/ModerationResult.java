package com.example.moderation.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Kết quả moderation từ provider (chuẩn hóa)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    /**
     * Tên provider đã xử lý (deepcleer, alibaba, mock)
     */
    private String providerName;

    /**
     * Mức độ rủi ro: LOW, MEDIUM, HIGH
     */
    private String riskLevel;

    /**
     * Độ tin cậy (0.0 - 1.0)
     */
    private Double confidenceScore;

    /**
     * Các nhãn phát hiện (spam, violence, sexual, etc.)
     */
    private List<String> labels;

    /**
     * Chi tiết bổ sung từ provider (provider-specific)
     */
    private Map<String, Object> details;

    /**
     * Raw JSON response từ API
     */
    private String rawResponse;

    /**
     * Thời gian xử lý (ms)
     */
    private Long latencyMs;

    /**
     * Thông báo lỗi (nếu có)
     */
    private String errorMessage;
}
