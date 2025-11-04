package com.example.moderation.config;

import com.aliyun.green20220302.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration cho Alibaba Cloud Content Moderation
 */
@Configuration
@ConfigurationProperties(prefix = "alibaba.cloud")
@Data
public class AlibabaCloudConfig {

    private String accessKeyId;
    private String accessKeySecret;
    private String regionId;
    private String endpoint;
    private String serviceName;
    private Integer readTimeout;
    private Integer connectTimeout;
    private Boolean mockMode;

    /**
     * Tạo Alibaba Cloud Client bean
     * Chỉ tạo khi không ở mock mode và có credentials
     */
    @Bean
    public Client alibabaClient() throws Exception {
        if (mockMode != null && mockMode) {
            // Trong mock mode, return null - service sẽ sử dụng mock implementation
            return null;
        }

        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setRegionId(regionId)
                .setEndpoint(endpoint)
                .setReadTimeout(readTimeout)
                .setConnectTimeout(connectTimeout);

        return new Client(config);
    }
}
