package com.example.moderation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class BeanConfig {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()  // Explicitly serialize nulls for debugging, but DeepCleer requires non-null strings
                .create();
    }

    /**
     * Gson instance for DeepCleer API requests that excludes null values
     * DeepCleer API requires fields to be either present with valid values or omitted entirely
     */
    @Bean("deepcleerGson")
    public Gson deepcleerGson() {
        return new GsonBuilder()
                // Don't serialize null values - DeepCleer API doesn't accept null for string fields
                .create();
    }

    /**
     * WebClient.Builder with proper timeout configuration
     * This is used by all providers including DeepCleer
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10s connect timeout
                .responseTimeout(Duration.ofSeconds(30)) // 30s response timeout
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
