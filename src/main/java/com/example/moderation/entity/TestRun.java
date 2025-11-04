package com.example.moderation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity để lưu trữ thông tin tổng hợp của mỗi lần chạy test
 */
@Entity
@Table(name = "test_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String runId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer totalRequests;

    @Column
    private Integer successCount;

    @Column
    private Integer failCount;

    @Column
    private Long avgLatencyMs;

    @Column
    private Long minLatencyMs;

    @Column
    private Long maxLatencyMs;

    @Column
    private Long p50LatencyMs;

    @Column
    private Long p95LatencyMs;

    @Column
    private Long p99LatencyMs;

    @Column
    private Double throughputRps;

    @Column
    private Integer concurrency;

    @Column(columnDefinition = "TEXT")
    private String configuration;

    @Column
    private String status; // RUNNING, COMPLETED, FAILED
}
