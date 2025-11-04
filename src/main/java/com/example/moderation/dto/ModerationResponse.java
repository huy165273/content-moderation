package com.example.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho response moderation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResponse {

    private String requestId;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private Double confidenceScore;
    private String rawResponse;
    private Long latencyMs;
    private Boolean success;
    private String errorMessage;
}
