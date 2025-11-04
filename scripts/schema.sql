-- SQLite Database Schema for Content Moderation Performance Testing
-- File: schema.sql
-- Description: Tạo tables để lưu trữ kết quả moderation và test metrics

-- Table: moderation_results
-- Lưu trữ kết quả chi tiết của từng request
CREATE TABLE IF NOT EXISTS moderation_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id VARCHAR(255) NOT NULL UNIQUE,
    run_id VARCHAR(255),
    payload TEXT,
    response_body TEXT,
    status_code INTEGER NOT NULL,
    latency_ms BIGINT NOT NULL,
    timestamp DATETIME NOT NULL,
    error_message TEXT,
    attempts INTEGER NOT NULL DEFAULT 1,
    success BOOLEAN NOT NULL DEFAULT 0,
    risk_level VARCHAR(50),
    confidence_score REAL
);

-- Indexes for moderation_results
CREATE INDEX IF NOT EXISTS idx_request_id ON moderation_results(request_id);
CREATE INDEX IF NOT EXISTS idx_run_id ON moderation_results(run_id);
CREATE INDEX IF NOT EXISTS idx_timestamp ON moderation_results(timestamp);
CREATE INDEX IF NOT EXISTS idx_success ON moderation_results(success);

-- Table: test_runs
-- Lưu trữ thông tin tổng hợp của mỗi test run
CREATE TABLE IF NOT EXISTS test_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id VARCHAR(255) NOT NULL UNIQUE,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    total_requests INTEGER NOT NULL,
    success_count INTEGER,
    fail_count INTEGER,
    avg_latency_ms BIGINT,
    min_latency_ms BIGINT,
    max_latency_ms BIGINT,
    p50_latency_ms BIGINT,
    p95_latency_ms BIGINT,
    p99_latency_ms BIGINT,
    throughput_rps REAL,
    concurrency INTEGER,
    configuration TEXT,
    status VARCHAR(50) DEFAULT 'RUNNING'
);

-- Index for test_runs
CREATE INDEX IF NOT EXISTS idx_test_run_id ON test_runs(run_id);
CREATE INDEX IF NOT EXISTS idx_test_start_time ON test_runs(start_time);
CREATE INDEX IF NOT EXISTS idx_test_status ON test_runs(status);

-- Sample queries for analysis
-- 1. Get all results for a specific run
-- SELECT * FROM moderation_results WHERE run_id = 'your-run-id' ORDER BY timestamp;

-- 2. Get summary statistics for a run
-- SELECT
--     COUNT(*) as total,
--     SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as success_count,
--     AVG(latency_ms) as avg_latency,
--     MIN(latency_ms) as min_latency,
--     MAX(latency_ms) as max_latency
-- FROM moderation_results
-- WHERE run_id = 'your-run-id';

-- 3. Get all test runs ordered by date
-- SELECT * FROM test_runs ORDER BY start_time DESC;
