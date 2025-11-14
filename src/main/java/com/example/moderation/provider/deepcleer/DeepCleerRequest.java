package com.example.moderation.provider.deepcleer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model cho DeepCleer API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepCleerRequest {

    private String accessKey;
    private String appId;
    private String eventId;
    private String type;
    private DataField data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataField {
        private String text;
        private String tokenId;
        private String channel;
    }
}
