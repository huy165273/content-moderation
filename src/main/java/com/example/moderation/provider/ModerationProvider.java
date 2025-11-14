package com.example.moderation.provider;

import java.util.Map;

/**
 * Interface cho content moderation providers
 * Hỗ trợ nhiều providers: DeepCleer, Alibaba, Mock, etc.
 */
public interface ModerationProvider {

    /**
     * Tên provider (deepcleer, alibaba, mock)
     */
    String getProviderName();

    /**
     * Moderate text content
     *
     * @param text Content cần moderate
     * @param options Tùy chọn bổ sung (language, categories, etc.)
     * @return Kết quả moderation
     * @throws ModerationException nếu có lỗi
     */
    ModerationResult moderateText(String text, Map<String, Object> options) throws ModerationException;

    /**
     * Health check của provider
     * @return true nếu provider đang hoạt động tốt
     */
    boolean isHealthy();

    /**
     * Lấy thông tin cấu hình của provider
     */
    ProviderConfig getConfig();
}
