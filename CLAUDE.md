# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.2 application for performance testing the **Alibaba Cloud Content Moderation API (Text Moderation 2.0 PLUS)**. It provides:

- REST API endpoints for content moderation
- Performance testing framework with configurable concurrency
- SQLite database for storing results and metrics
- Mock mode for local testing without API credentials
- Load testing scripts (Python)
- Comprehensive metrics and reporting

## Build and Development Commands

### Building the Project
```bash
mvn clean install
```

### Running the Application
```bash
# Mock mode (default, no credentials needed)
mvn spring-boot:run

# With real Alibaba Cloud API
export MOCK_MODE=false
export ALIBABA_ACCESS_KEY_ID=your-key-id
export ALIBABA_ACCESS_KEY_SECRET=your-key-secret
mvn spring-boot:run
```

### Running with Docker
```bash
# Build image
docker build -t content-moderation:latest .

# Run container
docker-compose up -d

# View logs
docker-compose logs -f
```

### Testing
```bash
# Run unit tests
mvn test

# Quick API test
bash scripts/quick_test.sh

# Load test (requires Python)
pip install requests
python scripts/load_test.py --requests 10 --concurrency 2
```

### Database Operations
```bash
# Initialize database
mkdir -p data
sqlite3 data/moderation_results.db < scripts/schema.sql

# Query results
sqlite3 data/moderation_results.db "SELECT * FROM test_runs ORDER BY start_time DESC LIMIT 5;"

# Clean database
rm data/moderation_results.db
```

## Project Structure

- **Group ID**: `com.example`
- **Artifact ID**: `content-moderation-perf`
- **Java Version**: 17
- **Framework**: Spring Boot 3.2.0
- **Database**: SQLite (file-based)
- **Main Class**: `com.example.moderation.ModerationApplication`

### Source Code Organization

```
src/main/java/com/example/moderation/
├── config/              # Spring configurations (Alibaba client, async, beans)
├── controller/          # REST endpoints (ModerationController, MetricsController)
├── dto/                 # Data Transfer Objects
├── entity/              # JPA entities (ModerationResult, TestRun)
├── repository/          # Spring Data repositories
└── service/             # Business logic (ContentModerationService, MetricsService)
```

## Code Architecture

### Key Components

1. **ContentModerationService**: Core service that calls Alibaba API (or mock). Handles retries, error handling, and saves results to database.

2. **MetricsService**: Calculates performance metrics (latency percentiles, throughput, success rate).

3. **ModerationController**: REST endpoints for moderating content (`/api/v1/moderate`, `/api/v1/moderate/batch`).

4. **MetricsController**: REST endpoints for viewing reports and metrics (`/api/v1/metrics/*`).

5. **AlibabaCloudConfig**: Configures Alibaba Cloud SDK client. Returns `null` in mock mode.

### Database Schema

- **moderation_results**: Stores individual request results (payload, response, latency, timestamp)
- **test_runs**: Stores aggregated metrics for each test run (p50/p95/p99 latencies, throughput)

### Configuration

Main config in `src/main/resources/application.yml`:
- Thread pool settings
- Alibaba Cloud credentials and endpoints
- Mock mode toggle
- Retry and timeout settings

## API Endpoints

### Moderation
- `POST /api/v1/moderate` - Moderate single text
- `POST /api/v1/moderate/batch?concurrency=N` - Moderate batch with concurrency

### Metrics
- `GET /api/v1/metrics/report/{runId}` - Get summary report for a run
- `GET /api/v1/metrics/report/{runId}/details` - Get detailed report with raw results
- `GET /api/v1/metrics/runs` - List all test runs
- `POST /api/v1/metrics/calculate/{runId}?concurrency=N` - Calculate and save metrics

### Health
- `GET /api/v1/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics

## Development Notes

### Mock vs Real API

- **Mock mode** (`MOCK_MODE=true`): Simulates API with 50-150ms latency. No credentials needed.
- **Real API mode** (`MOCK_MODE=false`): Calls actual Alibaba Cloud API. Requires valid credentials.

Toggle in `application.yml` or via environment variable.

### Concurrency Configuration

Adjust in `application.yml`:
```yaml
spring.task.execution.pool.max-size: 200  # Max async threads
server.tomcat.threads.max: 200             # Max HTTP threads
performance.default-concurrency: 10        # Default concurrency for batch
```

### Load Testing

Use `scripts/load_test.py`:
```bash
# Quick test
python scripts/load_test.py --requests 10 --concurrency 2

# Stress test
python scripts/load_test.py --requests 1000 --concurrency 200

# With rate limiting
python scripts/load_test.py --requests 500 --concurrency 50 --rate-limit 50
```

### Security Notes

- Never commit `.env` file (contains credentials)
- Database stores raw user text - handle PII carefully
- Add authentication to REST endpoints before production deployment
- Use encrypted connections for production