# Content Moderation Performance Testing Framework

## Mục tiêu

Project này là một framework để test performance của **Alibaba Cloud Content Moderation API (Text Moderation 2.0 PLUS)** với khả năng:

- ✅ Gửi lượng lớn requests đồng thời (concurrent requests)
- ✅ Đo và lưu trữ latency, throughput metrics
- ✅ Tính toán percentiles (P50, P95, P99)
- ✅ Lưu kết quả vào SQLite database
- ✅ Hỗ trợ mock mode để test local mà không cần API credentials
- ✅ Cấu hình được concurrency, rate limiting, timeout
- ✅ REST API để xem reports và metrics
- ✅ Load test scripts có thể tùy chỉnh

## Yêu cầu môi trường

### Tối thiểu
- **Java**: 17+
- **Maven**: 3.6+
- **Python**: 3.7+ (cho load test script)

### Tùy chọn (cho Docker)
- **Docker**: 20.10+
- **Docker Compose**: 1.29+

## Cấu trúc thư mục

```
Content-moderation/
├── src/
│   └── main/
│       ├── java/com/example/moderation/
│       │   ├── config/          # Configuration classes
│       │   ├── controller/      # REST Controllers
│       │   ├── dto/             # Data Transfer Objects
│       │   ├── entity/          # JPA Entities
│       │   ├── repository/      # Spring Data Repositories
│       │   └── service/         # Business Logic Services
│       └── resources/
│           └── application.yml  # Application configuration
├── scripts/
│   ├── schema.sql              # Database schema
│   ├── load_test.py            # Python load test script
│   └── quick_test.sh           # Quick API test script
├── data/                       # SQLite database (auto-created)
├── pom.xml                     # Maven dependencies
├── Dockerfile                  # Docker image definition
├── docker-compose.yml          # Docker Compose configuration
└── README.md                   # This file
```

## Thiết lập và cài đặt

### 1. Clone repository

```bash
cd D:\Code\OME\Content-moderation
```

### 2. Cấu hình credentials (nếu dùng real API)

Tạo file `.env` từ template:

```bash
cp .env.example .env
```

Chỉnh sửa `.env` và điền credentials:

```bash
# Lấy từ: https://ram.console.aliyun.com/manage/ak
ALIBABA_ACCESS_KEY_ID=your-actual-access-key-id
ALIBABA_ACCESS_KEY_SECRET=your-actual-access-key-secret

# Set to false để dùng real API
MOCK_MODE=false
```

⚠️ **LƯU Ý**: Không commit file `.env` vào git!

### 3. Khởi tạo database

Database SQLite sẽ tự động được tạo khi chạy application lần đầu. Nếu muốn tạo thủ công:

```bash
mkdir -p data
sqlite3 data/moderation_results.db < scripts/schema.sql
```

## Cách chạy

### Option 1: Chạy local với Maven

#### Build project

```bash
mvn clean install
```

#### Chạy application

```bash
# Mock mode (không cần credentials)
mvn spring-boot:run

# Real API mode
export MOCK_MODE=false
export ALIBABA_ACCESS_KEY_ID=your-key-id
export ALIBABA_ACCESS_KEY_SECRET=your-key-secret
mvn spring-boot:run
```

Application sẽ start ở `http://localhost:8080`

### Option 2: Chạy với Docker

#### Build Docker image

```bash
docker build -t content-moderation:latest .
```

#### Chạy container

```bash
# Mock mode
docker run -p 8080:8080 \
  -e MOCK_MODE=true \
  -v $(pwd)/data:/app/data \
  content-moderation:latest

# Real API mode
docker run -p 8080:8080 \
  -e MOCK_MODE=false \
  -e ALIBABA_ACCESS_KEY_ID=your-key \
  -e ALIBABA_ACCESS_KEY_SECRET=your-secret \
  -v $(pwd)/data:/app/data \
  content-moderation:latest
```

### Option 3: Chạy với Docker Compose

```bash
# Chỉnh .env file trước
docker-compose up -d

# Xem logs
docker-compose logs -f

# Stop
docker-compose down
```

## API Endpoints

### 1. Moderation Endpoints

#### Moderate một text đơn lẻ

```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{
    "id": "req-001",
    "text": "This is a test message",
    "runId": "test-run-1"
  }'
```

**Response:**

```json
{
  "requestId": "req-001",
  "riskLevel": "LOW",
  "confidenceScore": 0.95,
  "rawResponse": "{...}",
  "latencyMs": 123,
  "success": true,
  "errorMessage": null
}
```

#### Moderate batch (nhiều requests)

```bash
curl -X POST "http://localhost:8080/api/v1/moderate/batch?concurrency=10" \
  -H "Content-Type: application/json" \
  -d '[
    {"id": "req-1", "text": "Text 1", "runId": "batch-1"},
    {"id": "req-2", "text": "Text 2", "runId": "batch-1"}
  ]'
```

### 2. Metrics & Reporting Endpoints

#### Lấy report theo runId

```bash
curl http://localhost:8080/api/v1/metrics/report/{runId}
```

#### Lấy detailed report (với raw results)

```bash
curl http://localhost:8080/api/v1/metrics/report/{runId}/details
```

#### Lấy tất cả test runs

```bash
curl http://localhost:8080/api/v1/metrics/runs
```

#### Lấy current metrics

```bash
curl http://localhost:8080/api/v1/metrics/current
```

### 3. Health & Actuator

```bash
# Health check
curl http://localhost:8080/api/v1/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# All actuator endpoints
curl http://localhost:8080/actuator
```

## Load Testing

### Sử dụng Python Load Test Script

Script `load_test.py` hỗ trợ cấu hình đầy đủ concurrency, total requests, rate limiting.

#### Cài đặt dependencies

```bash
pip install requests
```

#### Ví dụ chạy load test

##### Test nhanh (10 requests, 2 concurrent)

```bash
python scripts/load_test.py --requests 10 --concurrency 2
```

##### Test trung bình (100 requests, 20 concurrent)

```bash
python scripts/load_test.py --requests 100 --concurrency 20
```

##### Test stress (1000 requests, 200 concurrent)

```bash
python scripts/load_test.py --requests 1000 --concurrency 200
```

##### Test với rate limiting (50 req/s max)

```bash
python scripts/load_test.py \
  --requests 500 \
  --concurrency 50 \
  --rate-limit 50
```

##### Custom URL và Run ID

```bash
python scripts/load_test.py \
  --url http://localhost:8080 \
  --requests 100 \
  --concurrency 10 \
  --run-id my-custom-run-001
```

#### Output mẫu

```
==========================================================
Starting Load Test
==========================================================

  Run ID:          run-20240115-143022-a3f5b2c8
  Target URL:      http://localhost:8080
  Total Requests:  100
  Concurrency:     20
  Rate Limit:      Unlimited req/s

==========================================================

Progress: 100.0% (100/100)

Test completed!

==========================================================
Test Results Summary
==========================================================

  Run ID:           run-20240115-143022-a3f5b2c8
  Duration:         5.23s
  Total Requests:   100
  Success:          98
  Failed:           2
  Success Rate:     98.00%

Latency (ms):
  Min:              52
  Max:              234
  Average:          87.45
  P50:              85
  P95:              156
  P99:              210

Throughput:
  Requests/sec:     19.12

==========================================================

✓ Metrics saved to database via API

To view detailed report:
  curl http://localhost:8080/api/v1/metrics/report/run-20240115-143022-a3f5b2c8
```

### Sử dụng Quick Test Script (Bash)

```bash
# Test với local server
bash scripts/quick_test.sh

# Test với custom URL
bash scripts/quick_test.sh http://your-server:8080
```

## Cấu hình Performance

### application.yml

Chỉnh sửa `src/main/resources/application.yml` để thay đổi:

```yaml
# Thread pool cho async processing
spring:
  task:
    execution:
      pool:
        core-size: 20       # Số threads tối thiểu
        max-size: 200       # Số threads tối đa
        queue-capacity: 500 # Hàng đợi tasks

# Tomcat configuration
server:
  tomcat:
    threads:
      max: 200             # Max HTTP threads
      min-spare: 20        # Min spare threads

# Alibaba Cloud timeouts
alibaba:
  cloud:
    read-timeout: 6000     # milliseconds
    connect-timeout: 3000  # milliseconds
    mock-mode: true        # true = mock, false = real API

# Performance settings
performance:
  default-concurrency: 10
  max-concurrency: 500
  rate-limit: 0            # 0 = unlimited
  retry:
    max-attempts: 3
    backoff-delay-ms: 1000
    backoff-multiplier: 2.0
```

### Khuyến nghị cấu hình theo use case

#### Test nhẹ (Development)
```yaml
spring.task.execution.pool.max-size: 50
server.tomcat.threads.max: 50
performance.default-concurrency: 10
```

#### Test trung bình (QA)
```yaml
spring.task.execution.pool.max-size: 100
server.tomcat.threads.max: 100
performance.default-concurrency: 50
```

#### Stress test (Production-like)
```yaml
spring.task.execution.pool.max-size: 200
server.tomcat.threads.max: 200
performance.default-concurrency: 200
```

## Database Schema

### Table: `moderation_results`

Lưu trữ kết quả từng request:

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| request_id | VARCHAR(255) | Unique request identifier |
| run_id | VARCHAR(255) | Test run group identifier |
| payload | TEXT | Request payload (JSON) |
| response_body | TEXT | API response (JSON) |
| status_code | INTEGER | HTTP status code |
| latency_ms | BIGINT | Request latency in milliseconds |
| timestamp | DATETIME | Request timestamp |
| error_message | TEXT | Error message (if failed) |
| attempts | INTEGER | Number of retry attempts |
| success | BOOLEAN | Success flag |
| risk_level | VARCHAR(50) | Moderation risk level |
| confidence_score | REAL | Confidence score |

### Table: `test_runs`

Lưu trữ metrics tổng hợp của mỗi test run:

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| run_id | VARCHAR(255) | Unique run identifier |
| start_time | DATETIME | Test start time |
| end_time | DATETIME | Test end time |
| total_requests | INTEGER | Total requests sent |
| success_count | INTEGER | Successful requests |
| fail_count | INTEGER | Failed requests |
| avg_latency_ms | BIGINT | Average latency |
| min_latency_ms | BIGINT | Minimum latency |
| max_latency_ms | BIGINT | Maximum latency |
| p50_latency_ms | BIGINT | 50th percentile latency |
| p95_latency_ms | BIGINT | 95th percentile latency |
| p99_latency_ms | BIGINT | 99th percentile latency |
| throughput_rps | REAL | Throughput (requests/sec) |
| concurrency | INTEGER | Concurrency level |
| status | VARCHAR(50) | Run status |

### Query examples

```sql
-- Xem tất cả results của một run
SELECT * FROM moderation_results
WHERE run_id = 'your-run-id'
ORDER BY timestamp;

-- Tính success rate
SELECT
  COUNT(*) as total,
  SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as success_count,
  ROUND(100.0 * SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) / COUNT(*), 2) as success_rate_pct
FROM moderation_results
WHERE run_id = 'your-run-id';

-- Xem latency distribution
SELECT
  MIN(latency_ms) as min,
  MAX(latency_ms) as max,
  AVG(latency_ms) as avg,
  COUNT(*) as total
FROM moderation_results
WHERE run_id = 'your-run-id';

-- Xem error summary
SELECT error_message, COUNT(*) as count
FROM moderation_results
WHERE run_id = 'your-run-id' AND success = 0
GROUP BY error_message;
```

## Mock Mode vs Real API

### Mock Mode

- ✅ Không cần Alibaba Cloud credentials
- ✅ Phù hợp cho development và testing infrastructure
- ✅ Simulates latency (50-150ms)
- ✅ Keyword-based risk detection (spam, illegal, bad → HIGH risk)
- ⚠️ Không gọi API thực tế

**Kích hoạt:**

```bash
# application.yml
alibaba.cloud.mock-mode: true

# hoặc environment variable
export MOCK_MODE=true
```

### Real API Mode

- ✅ Gọi Alibaba Cloud Content Moderation API thực tế
- ✅ Kết quả moderation chính xác
- ⚠️ Yêu cầu credentials hợp lệ
- ⚠️ Có thể bị charge phí (check Alibaba pricing)
- ⚠️ Có quota/rate limits

**Kích hoạt:**

```bash
# application.yml
alibaba.cloud.mock-mode: false

# Set credentials
export ALIBABA_ACCESS_KEY_ID=your-key-id
export ALIBABA_ACCESS_KEY_SECRET=your-key-secret
```

## Monitoring & Observability

### Prometheus Integration

Application expose Prometheus metrics tại `/actuator/prometheus`

```bash
curl http://localhost:8080/actuator/prometheus
```

### Kết nối Grafana (Optional)

1. Uncomment Prometheus & Grafana services trong `docker-compose.yml`
2. Tạo file `docker/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'content-moderation'
    static_configs:
      - targets: ['content-moderation:8080']
    metrics_path: '/actuator/prometheus'
```

3. Start services:

```bash
docker-compose up -d
```

4. Access Grafana: http://localhost:3000 (admin/admin)
5. Add Prometheus datasource: http://prometheus:9090
6. Import dashboard hoặc tạo custom dashboard với metrics

### Logs

Application sử dụng SLF4J + Logback với trace ID:

```bash
# Xem logs realtime
docker-compose logs -f content-moderation

# Với Maven
mvn spring-boot:run

# Logs bao gồm traceId để track từng request
2024-01-15 14:30:22.123 [http-nio-8080-exec-1] DEBUG c.e.m.s.ContentModerationService - Processing moderation request: req-001 - traceId=a3f5b2c8-...
```

## Edge Cases & Production Notes

### Rate Limiting & Quota

- Alibaba Cloud có **rate limits** và **quota** cho API calls
- Trong production, nên implement:
  - Circuit breaker (đã có config, có thể enhance)
  - Exponential backoff retry (đã implement)
  - Request queuing với bounded queue

### Throttling Detection

- Nếu API trả về HTTP 429 (Too Many Requests), service sẽ retry với backoff
- Monitor fail rate để detect khi hit rate limit

### Database Size Management

SQLite file sẽ tăng khi có nhiều results. Để quản lý:

```bash
# Xóa old results (older than 30 days)
sqlite3 data/moderation_results.db "DELETE FROM moderation_results WHERE timestamp < datetime('now', '-30 days');"

# Vacuum để shrink database
sqlite3 data/moderation_results.db "VACUUM;"

# Hoặc xóa toàn bộ và reset
rm data/moderation_results.db
```

### Security & Privacy

⚠️ **QUAN TRỌNG**: Database lưu **raw text** của user content!

**Khuyến nghị:**

1. **Không log sensitive data** vào production logs
2. **Encrypt database** nếu chứa PII (Personally Identifiable Information)
3. **GDPR compliance**: Implement data retention policy và right to be forgotten
4. **Access control**: Restrict database file permissions

```bash
# Set restrictive permissions
chmod 600 data/moderation_results.db
```

5. **Không expose database** qua public network
6. **API authentication**: Thêm authentication cho REST endpoints trong production

### Connection Pooling

- Spring Boot tự động configure HikariCP cho JPA
- Với SQLite, connection pooling limited (SQLite = file-based)
- Trong production với high concurrency, nên dùng:
  - PostgreSQL, MySQL, hoặc
  - Distributed cache (Redis) cho hot data

### Circuit Breaker Configuration

Enhance `CircuitBreaker` trong production:

```java
// Add dependency
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot2</artifactId>
</dependency>

// Configure in application.yml
resilience4j.circuitbreaker:
  instances:
    alibaba-api:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 10s
      sliding-window-size: 10
```

## Troubleshooting

### Application không start

```bash
# Check Java version
java -version  # Cần >= 17

# Check port 8080 availability
netstat -an | grep 8080

# Check logs
mvn spring-boot:run
# hoặc
docker-compose logs content-moderation
```

### Database errors

```bash
# Recreate database
rm data/moderation_results.db
mkdir -p data
sqlite3 data/moderation_results.db < scripts/schema.sql
```

### Load test script fails

```bash
# Check Python version
python3 --version

# Install dependencies
pip install requests

# Check app is running
curl http://localhost:8080/api/v1/health
```

### Real API authentication fails

```bash
# Verify credentials
echo $ALIBABA_ACCESS_KEY_ID
echo $ALIBABA_ACCESS_KEY_SECRET

# Check API endpoint reachable
curl -v https://green-cip.ap-southeast-1.aliyuncs.com

# Enable debug logs
# In application.yml:
logging.level.com.aliyun: DEBUG
```

## Development Workflow Checklist

Checklist để thực hiện từng bước:

- [ ] 1. Clone/setup project
- [ ] 2. Install Java 17+ và Maven
- [ ] 3. (Optional) Install Docker & Docker Compose
- [ ] 4. Copy `.env.example` to `.env`
- [ ] 5. Set `MOCK_MODE=true` (hoặc configure real credentials)
- [ ] 6. Build project: `mvn clean install`
- [ ] 7. Start application: `mvn spring-boot:run` hoặc `docker-compose up`
- [ ] 8. Verify health: `curl http://localhost:8080/api/v1/health`
- [ ] 9. Run quick test: `bash scripts/quick_test.sh`
- [ ] 10. Install Python dependencies: `pip install requests`
- [ ] 11. Run load test: `python scripts/load_test.py --requests 10 --concurrency 2`
- [ ] 12. Check metrics: `curl http://localhost:8080/api/v1/metrics/current`
- [ ] 13. View report: `curl http://localhost:8080/api/v1/metrics/report/{runId}`
- [ ] 14. Inspect database: `sqlite3 data/moderation_results.db "SELECT * FROM test_runs;"`
- [ ] 15. (Optional) Run stress test: `python scripts/load_test.py --requests 1000 --concurrency 200`
- [ ] 16. (Production) Set `MOCK_MODE=false` và configure real credentials
- [ ] 17. (Production) Implement authentication cho API endpoints
- [ ] 18. (Production) Setup monitoring (Prometheus + Grafana)

## Thiết kế Sync vs Async

### Lựa chọn hiện tại: **Hybrid**

#### REST Controller: **Sync**

- `POST /api/v1/moderate`: Blocking, đợi response
- Phù hợp cho: Single requests, debugging
- **Ưu điểm**: Đơn giản, dễ debug
- **Nhược điểm**: Không scale tốt với high concurrency

#### Batch Endpoint: **Async với Thread Pool**

- `POST /api/v1/moderate/batch`: Sử dụng `CompletableFuture` + `ExecutorService`
- **Ưu điểm**:
  - Handle nhiều requests đồng thời
  - Non-blocking processing
  - Configurable concurrency
- **Nhược điểm**:
  - Phức tạp hơn
  - Cần quản lý thread pool

#### Load Test Script: **Async với Python Threading**

- Python `concurrent.futures.ThreadPoolExecutor`
- **Ưu điểm**:
  - Flexible concurrency control
  - Real-world load simulation
  - Easy rate limiting

### Khuyến nghị Scale-up

Nếu cần scale hơn nữa:

1. **WebFlux Reactive**: Migrate to Spring WebFlux cho fully non-blocking
2. **Message Queue**: Sử dụng RabbitMQ/Kafka cho async processing
3. **Load Balancer**: Deploy multiple instances + Nginx/HAProxy

## Tài liệu tham khảo

- [Alibaba Cloud Content Moderation Documentation](https://www.alibabacloud.com/help/en/content-moderation/latest/access-guide)
- [Alibaba Cloud Text Moderation 2.0 PLUS Guide](https://www.alibabacloud.com/help/en/content-moderation/latest/access-guide?spm=a2c63.p38356.help-menu-28415.d_1_0.1e8f7171pTyb8O)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Micrometer Metrics](https://micrometer.io/)

## License

MIT License - Free to use and modify

## Support

Nếu gặp vấn đề, tạo issue hoặc liên hệ team.

---

**Tác giả**: Generated with Claude Code
**Version**: 1.0.0
**Last Updated**: 2024-01-15
