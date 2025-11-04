package com.example.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho performance metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {

    private String runId;
    private Integer totalRequests;
    private Integer successCount;
    private Integer failCount;
    private Double successRate;

    // Latency metrics (milliseconds)
    private Long minLatency;
    private Long maxLatency;
    private Long avgLatency;
    private Long p50Latency;
    private Long p95Latency;
    private Long p99Latency;

    // Throughput
    private Double throughputRps;
    private Long durationMs;

    // Configuration
    private Integer concurrency;
    private String startTime;
    private String endTime;
}
