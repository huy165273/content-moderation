package com.example.moderation.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory để lấy ModerationProvider theo tên
 * Auto-detect tất cả providers trong Spring context
 */
@Component
@Slf4j
public class ModerationProviderFactory {

    private final Map<String, ModerationProvider> providerMap;

    public ModerationProviderFactory(List<ModerationProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        ModerationProvider::getProviderName,
                        Function.identity()
                ));

        log.info("Initialized ModerationProviderFactory with {} providers: {}",
                providerMap.size(),
                providerMap.keySet());
    }

    /**
     * Lấy provider theo tên
     *
     * @param providerName Tên provider (deepcleer, alibaba, mock)
     * @return ModerationProvider instance
     * @throws IllegalArgumentException nếu provider không tồn tại
     */
    public ModerationProvider getProvider(String providerName) {
        ModerationProvider provider = providerMap.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException(
                    String.format("Unknown provider: %s. Available providers: %s",
                            providerName, providerMap.keySet())
            );
        }
        return provider;
    }

    /**
     * Lấy tất cả providers có sẵn
     */
    public List<ModerationProvider> getAllProviders() {
        return List.copyOf(providerMap.values());
    }

    /**
     * Kiểm tra provider có tồn tại không
     */
    public boolean hasProvider(String providerName) {
        return providerMap.containsKey(providerName);
    }
}
