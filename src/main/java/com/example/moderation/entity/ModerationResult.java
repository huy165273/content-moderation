package com.example.moderation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity để lưu trữ kết quả từng request moderation
 */
@Entity
@Table(name = "moderation_results", indexes = {
        @Index(name = "idx_request_id", columnList = "requestId"),
        @Index(name = "idx_run_id", columnList = "runId"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String requestId;

    @Column(name = "run_id")
    private String runId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Column(nullable = false)
    private Integer statusCode;

    @Column(nullable = false)
    private Long latencyMs;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Integer attempts;

    @Column
    private Boolean success;

    @Column
    private String riskLevel;

    @Column
    private Double confidenceScore;
}
