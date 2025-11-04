package com.example.moderation.repository;

import com.example.moderation.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    Optional<TestRun> findByRunId(String runId);

    Optional<TestRun> findFirstByOrderByStartTimeDesc();
}
