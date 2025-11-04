package com.example.moderation.service;

import com.example.moderation.dto.PerformanceMetrics;
import com.example.moderation.entity.ModerationResult;
import com.example.moderation.entity.TestRun;
import com.example.moderation.repository.ModerationResultRepository;
import com.example.moderation.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service để tính toán và lưu trữ performance metrics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    private final ModerationResultRepository resultRepository;
    private final TestRunRepository testRunRepository;

    /**
     * Tính toán metrics cho một test run
     */
    public PerformanceMetrics calculateMetrics(String runId) {
        List<ModerationResult> results = resultRepository.findByRunIdOrderByTimestampAsc(runId);

        if (results.isEmpty()) {
            return null;
        }

        // Lấy latencies và sort
        List<Long> latencies = results.stream()
                .map(ModerationResult::getLatencyMs)
                .sorted()
                .collect(Collectors.toList());

        int total = results.size();
        long successCount = results.stream().filter(ModerationResult::getSuccess).count();
        long failCount = total - successCount;

        // Calculate percentiles
        long p50 = calculatePercentile(latencies, 50);
        long p95 = calculatePercentile(latencies, 95);
        long p99 = calculatePercentile(latencies, 99);

        // Calculate duration và throughput
        LocalDateTime startTime = results.get(0).getTimestamp();
        LocalDateTime endTime = results.get(results.size() - 1).getTimestamp();
        long durationMs = Duration.between(startTime, endTime).toMillis();
        double throughput = durationMs > 0 ? (total * 1000.0) / durationMs : 0;

        Long minLatency = resultRepository.findMinLatencyByRunId(runId);
        Long maxLatency = resultRepository.findMaxLatencyByRunId(runId);
        Double avgLatency = resultRepository.findAvgLatencyByRunId(runId);

        return PerformanceMetrics.builder()
                .runId(runId)
                .totalRequests(total)
                .successCount((int) successCount)
                .failCount((int) failCount)
                .successRate((double) successCount / total * 100)
                .minLatency(minLatency)
                .maxLatency(maxLatency)
                .avgLatency(avgLatency != null ? avgLatency.longValue() : 0)
                .p50Latency(p50)
                .p95Latency(p95)
                .p99Latency(p99)
                .throughputRps(throughput)
                .durationMs(durationMs)
                .startTime(formatDateTime(startTime))
                .endTime(formatDateTime(endTime))
                .build();
    }

    /**
     * Lưu metrics vào TestRun table
     */
    public void saveTestRun(String runId, PerformanceMetrics metrics, int concurrency) {
        TestRun testRun = TestRun.builder()
                .runId(runId)
                .startTime(LocalDateTime.parse(metrics.getStartTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .endTime(LocalDateTime.parse(metrics.getEndTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .totalRequests(metrics.getTotalRequests())
                .successCount(metrics.getSuccessCount())
                .failCount(metrics.getFailCount())
                .avgLatencyMs(metrics.getAvgLatency())
                .minLatencyMs(metrics.getMinLatency())
                .maxLatencyMs(metrics.getMaxLatency())
                .p50LatencyMs(metrics.getP50Latency())
                .p95LatencyMs(metrics.getP95Latency())
                .p99LatencyMs(metrics.getP99Latency())
                .throughputRps(metrics.getThroughputRps())
                .concurrency(concurrency)
                .status("COMPLETED")
                .build();

        testRunRepository.save(testRun);
        log.info("Saved test run metrics for runId: {}", runId);
    }

    /**
     * Calculate percentile
     */
    private long calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }

        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
