package com.example.moderation.controller;

import com.example.moderation.dto.PerformanceMetrics;
import com.example.moderation.entity.ModerationResult;
import com.example.moderation.entity.TestRun;
import com.example.moderation.exception.EntityNotFoundException;
import com.example.moderation.repository.ModerationResultRepository;
import com.example.moderation.repository.TestRunRepository;
import com.example.moderation.service.MetricsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho metrics và reporting
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Slf4j
@Validated // Enable validation for @PathVariable and @RequestParam
public class MetricsController {

    private final MetricsService metricsService;
    private final TestRunRepository testRunRepository;
    private final ModerationResultRepository resultRepository;

    /**
     * Lấy metrics tổng hợp theo runId
     */
    @GetMapping("/report/{runId}")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable String runId) {
        log.info("Fetching report for runId: {}", runId);

        PerformanceMetrics metrics = metricsService.calculateMetrics(runId);
        if (metrics == null) {
            throw new EntityNotFoundException("TestRun", runId);
        }

        List<ModerationResult> results = resultRepository.findByRunId(runId);

        Map<String, Object> report = new HashMap<>();
        report.put("metrics", metrics);
        report.put("totalResults", results.size());
        report.put("runId", runId);

        return ResponseEntity.ok(report);
    }

    /**
     * Lấy metrics chi tiết kèm raw results
     */
    @GetMapping("/report/{runId}/details")
    public ResponseEntity<Map<String, Object>> getDetailedReport(@PathVariable String runId) {
        log.info("Fetching detailed report for runId: {}", runId);

        PerformanceMetrics metrics = metricsService.calculateMetrics(runId);
        if (metrics == null) {
            throw new EntityNotFoundException("TestRun", runId);
        }

        List<ModerationResult> results = resultRepository.findByRunIdOrderByTimestampAsc(runId);

        Map<String, Object> report = new HashMap<>();
        report.put("metrics", metrics);
        report.put("results", results);

        return ResponseEntity.ok(report);
    }

    /**
     * Lấy danh sách tất cả test runs
     */
    @GetMapping("/runs")
    public ResponseEntity<List<TestRun>> getAllRuns() {
        List<TestRun> runs = testRunRepository.findAll();
        return ResponseEntity.ok(runs);
    }

    /**
     * Lấy test run theo ID
     */
    @GetMapping("/runs/{runId}")
    public ResponseEntity<TestRun> getRunById(@PathVariable String runId) {
        return testRunRepository.findByRunId(runId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("TestRun", runId));
    }

    /**
     * Tính và lưu metrics cho một run
     */
    @PostMapping("/calculate/{runId}")
    public ResponseEntity<PerformanceMetrics> calculateAndSaveMetrics(
            @PathVariable String runId,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Concurrency phải >= 1")
            @Max(value = 500, message = "Concurrency không được vượt quá 500")
            int concurrency) {

        PerformanceMetrics metrics = metricsService.calculateMetrics(runId);
        if (metrics == null) {
            throw new EntityNotFoundException("TestRun", runId);
        }

        metricsService.saveTestRun(runId, metrics, concurrency);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Endpoint để xem metrics hiện tại (Prometheus format tương thích)
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentMetrics() {
        // Lấy run mới nhất
        return testRunRepository.findFirstByOrderByStartTimeDesc()
                .map(testRun -> {
                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put("total_requests", testRun.getTotalRequests());
                    metrics.put("success_count", testRun.getSuccessCount());
                    metrics.put("fail_count", testRun.getFailCount());
                    metrics.put("avg_latency_ms", testRun.getAvgLatencyMs());
                    metrics.put("p95_latency_ms", testRun.getP95LatencyMs());
                    metrics.put("p99_latency_ms", testRun.getP99LatencyMs());
                    metrics.put("throughput_rps", testRun.getThroughputRps());
                    return ResponseEntity.ok(metrics);
                })
                .orElse(ResponseEntity.ok(new HashMap<>()));
    }
}
