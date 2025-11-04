package com.example.moderation.repository;

import com.example.moderation.entity.ModerationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationResultRepository extends JpaRepository<ModerationResult, Long> {

    List<ModerationResult> findByRunId(String runId);

    List<ModerationResult> findByRunIdOrderByTimestampAsc(String runId);

    @Query("SELECT MIN(r.latencyMs) FROM ModerationResult r WHERE r.runId = :runId")
    Long findMinLatencyByRunId(@Param("runId") String runId);

    @Query("SELECT MAX(r.latencyMs) FROM ModerationResult r WHERE r.runId = :runId")
    Long findMaxLatencyByRunId(@Param("runId") String runId);

    @Query("SELECT AVG(r.latencyMs) FROM ModerationResult r WHERE r.runId = :runId")
    Double findAvgLatencyByRunId(@Param("runId") String runId);

    @Query("SELECT COUNT(r) FROM ModerationResult r WHERE r.runId = :runId AND r.success = true")
    Long countSuccessByRunId(@Param("runId") String runId);

    @Query("SELECT COUNT(r) FROM ModerationResult r WHERE r.runId = :runId AND r.success = false")
    Long countFailuresByRunId(@Param("runId") String runId);
}
