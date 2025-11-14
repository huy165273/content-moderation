package com.example.moderation.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cấu hình provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfig {

    /**
     * Tên provider
     */
    private String providerName;

    /**
     * Base URL của API
     */
    private String baseUrl;

    /**
     * Timeout (ms)
     */
    private Integer timeout;

    /**
     * Enabled flag
     */
    private Boolean enabled;
}
