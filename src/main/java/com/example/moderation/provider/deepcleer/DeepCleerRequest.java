package com.example.moderation.provider.deepcleer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request model cho DeepCleer API
 * Theo tài liệu: http://api-text-bj.fengkongcloud.com/text/v4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepCleerRequest {

    /**
     * Authentication key for the API (Required)
     */
    private String accessKey;

    /**
     * Application identifier (Required)
     */
    private String appId;

    /**
     * Event identifier (Required)
     */
    private String eventId;

    /**
     * Type of risk detection (Required)
     * Example: TEXTRISK, FRAUD, TEXTMINOR
     */
    private String type;

    /**
     * Content of the request data (Required)
     */
    private DataField data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataField {
        /**
         * Text content to moderate (Required)
         */
        private String text;

        /**
         * User token ID (Optional)
         */
        private String tokenId;

        /**
         * IP address (Optional)
         */
        private String ip;

        /**
         * Device ID (Optional)
         */
        private String deviceId;

        /**
         * User nickname (Optional)
         */
        private String nickname;

        /**
         * Additional metadata (Optional)
         * Can include: topic, atId, room, receiveTokenId
         */
        private Map<String, Object> extra;
    }
}
