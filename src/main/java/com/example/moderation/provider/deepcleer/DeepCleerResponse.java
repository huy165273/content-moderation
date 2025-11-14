package com.example.moderation.provider.deepcleer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model cho DeepCleer API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepCleerResponse {

    private Integer code;
    private String message;
    private String riskLevel;  // PASS, REVIEW, REJECT
    private Double score;
    private Detail detail;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private List<Hit> hits;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hit {
        private String label;
        private Double confidence;
        private String description;
    }
}
