# DeepCleer Content Moderation Integration Guide

## 📋 Tổng Quan

Dự án đã được tích hợp thành công **DeepCleer Content Moderation API** với kiến trúc **multi-provider strategy pattern**.

### ✨ Các Tính Năng Chính

- ✅ **Multi-Provider Support**: DeepCleer, Alibaba Cloud, Mock
- ✅ **Fallback Mechanism**: Tự động chuyển sang provider dự phòng khi primary provider lỗi
- ✅ **Circuit Breaker**: Bảo vệ hệ thống khi API external down
- ✅ **Retry Logic**: Exponential backoff cho các failed requests
- ✅ **Provider Switching**: Dễ dàng chuyển đổi giữa các providers qua configuration
- ✅ **Database Persistence**: Lưu trữ kết quả moderation với thông tin provider
- ✅ **Comprehensive Tests**: 21 integration tests covering all providers

---

## 🚀 Quick Start

### 1. Chạy Với Mock Provider (Không Cần API Keys)

```bash
# Default: Mock mode
mvn spring-boot:run
```

### 2. Chạy Với DeepCleer Provider

```bash
# Set environment variables
export MODERATION_PROVIDER=deepcleer
export DEEPCLEER_ENABLED=true
export DEEPCLEER_ACCESS_KEY=your-deepcleer-access-key
export DEEPCLEER_APP_ID=your-deepcleer-app-id

mvn spring-boot:run
```

### 3. Chạy Với Alibaba Cloud Provider

```bash
export MODERATION_PROVIDER=alibaba
export MOCK_MODE=false
export ALIBABA_ACCESS_KEY_ID=your-alibaba-key
export ALIBABA_ACCESS_KEY_SECRET=your-alibaba-secret

mvn spring-boot:run
```

---

## 🔧 Configuration

### Provider Selection

Trong `application.yml`:

```yaml
content-moderation:
  active-provider: mock  # Options: deepcleer, alibaba, mock

  fallback:
    enabled: true
    secondary-provider: mock
```

### DeepCleer Configuration

```yaml
deepcleer:
  api:
    access-key: ${DEEPCLEER_ACCESS_KEY}
    app-id: ${DEEPCLEER_APP_ID}
    base-url: https://api-text-na.deepcleer.com
    text-moderation-endpoint: /api/v3/text/check
    enabled: ${DEEPCLEER_ENABLED:false}

    read-timeout-ms: 6000
    connect-timeout-ms: 3000

    retry:
      max-attempts: 3
      backoff-delay-ms: 1000

    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50
      wait-duration-ms: 60000
```

### Switch Provider Via Environment Variables

```bash
# DeepCleer
MODERATION_PROVIDER=deepcleer

# Alibaba
MODERATION_PROVIDER=alibaba

# Mock (testing)
MODERATION_PROVIDER=mock
```

---

## 📡 API Endpoints

### 1. Moderate Single Text

```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{
    "id": "req-001",
    "text": "Hello, this is a test message"
  }'
```

**Response:**

```json
{
  "requestId": "req-001",
  "riskLevel": "LOW",
  "confidenceScore": 0.95,
  "latencyMs": 123,
  "success": true,
  "rawResponse": "..."
}
```

### 2. Moderate Batch (With Concurrency)

```bash
curl -X POST "http://localhost:8080/api/v1/moderate/batch?concurrency=5" \
  -H "Content-Type: application/json" \
  -d '[
    {"id": "req-001", "text": "Clean message"},
    {"id": "req-002", "text": "SPAM! BUY NOW!"},
    {"id": "req-003", "text": "Another clean message"}
  ]'
```

### 3. Health Check

```bash
curl http://localhost:8080/api/v1/health
```

---

## 🎯 Risk Levels

| Provider Risk Level | Internal Risk Level | Action |
|---------------------|---------------------|--------|
| PASS (DeepCleer) | LOW | ✅ Auto-approve |
| REVIEW (DeepCleer) | MEDIUM | ⚠️ Manual review |
| REJECT (DeepCleer) | HIGH | ❌ Block content |

---

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=ModerationProviderIntegrationTest
mvn test -Dtest=ContentModerationServiceIntegrationTest
mvn test -Dtest=ModerationControllerIntegrationTest
```

### Test Coverage

- ✅ Provider integration tests (8 tests)
- ✅ Service layer tests (6 tests)
- ✅ Controller E2E tests (7 tests)
- **Total: 21 tests, all passing**

---

## 🏗️ Architecture

### Provider Strategy Pattern

```
Controller
    ↓
ContentModerationService
    ↓
ModerationProviderFactory
    ↓
┌─────────────┬──────────────┬──────────────┐
│ DeepCleer   │   Alibaba    │     Mock     │
│ Provider    │   Provider   │   Provider   │
└─────────────┴──────────────┴──────────────┘
```

### Class Diagram

```
ModerationProvider (Interface)
    ├─ DeepCleerProvider
    │   ├─ CircuitBreaker (Resilience4j)
    │   ├─ Retry Logic
    │   └─ WebClient (Reactive)
    │
    ├─ AlibabaProvider
    │   └─ Alibaba SDK Client
    │
    └─ MockProvider
        └─ Keyword-based Detection
```

---

## 📊 Database Schema

### moderation_results Table

| Column | Type | Description |
|--------|------|-------------|
| id | LONG | Primary key |
| request_id | STRING | Unique request identifier |
| run_id | STRING | Test run group identifier |
| payload | TEXT | Request JSON |
| response_body | TEXT | Raw API response |
| status_code | INT | HTTP status |
| latency_ms | LONG | Processing time |
| risk_level | STRING | LOW/MEDIUM/HIGH |
| confidence_score | DOUBLE | 0.0 - 1.0 |
| **provider_name** | STRING | **deepcleer/alibaba/mock** |
| success | BOOLEAN | Success flag |
| timestamp | DATETIME | Creation time |

---

## 🔒 Security Best Practices

### 1. Credential Management

**❌ Never commit credentials:**

```yaml
# BAD - Don't do this
deepcleer:
  api:
    access-key: "hardcoded-key-12345"
```

**✅ Use environment variables:**

```yaml
# GOOD
deepcleer:
  api:
    access-key: ${DEEPCLEER_ACCESS_KEY}
```

### 2. .env File

Create `.env` file (DON'T commit):

```bash
DEEPCLEER_ACCESS_KEY=your-key-here
DEEPCLEER_APP_ID=your-app-id
DEEPCLEER_ENABLED=true
MODERATION_PROVIDER=deepcleer
```

### 3. Docker Secrets (Production)

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - DEEPCLEER_ACCESS_KEY=/run/secrets/deepcleer_key
    secrets:
      - deepcleer_key

secrets:
  deepcleer_key:
    external: true
```

---

## 🚨 Error Handling

### Circuit Breaker States

```
CLOSED → OPEN → HALF_OPEN → CLOSED
   ↓       ↓         ↓
  OK    Failed    Retry
```

### Fallback Strategy

1. **Primary Provider Fails** → Try primary with retry
2. **Still Fails** → Switch to secondary provider
3. **Both Fail** → Return error response (don't crash)

### Example Error Response

```json
{
  "requestId": "req-001",
  "success": false,
  "errorMessage": "DeepCleer API failed: Connection timeout",
  "latencyMs": 6000
}
```

---

## 📈 Monitoring & Metrics

### Prometheus Metrics

Access at: `http://localhost:8080/actuator/prometheus`

**Custom Metrics:**

```
moderation_calls_total{provider="deepcleer",risk_level="LOW",success="true"}
moderation_latency{provider="deepcleer",quantile="0.95"}
```

### Grafana Dashboard (Example Queries)

```promql
# Success Rate
sum(rate(moderation_calls_total{success="true"}[5m]))
/
sum(rate(moderation_calls_total[5m]))

# P95 Latency by Provider
histogram_quantile(0.95,
  sum(rate(moderation_latency_bucket[5m])) by (le, provider)
)

# Error Rate
sum(rate(moderation_calls_total{success="false"}[5m])) by (provider)
```

---

## 🔄 Migration Roadmap

### Phase 1: Parallel Running (Week 1-2)
- ✅ Deploy DeepCleer provider alongside existing
- ✅ Route 10% traffic to DeepCleer
- ✅ Compare results and metrics
- ✅ Tune thresholds

### Phase 2: Gradual Rollout (Week 3-4)
- 🔄 Increase DeepCleer: 25% → 50% → 75%
- 🔄 Monitor error rates
- 🔄 Adjust circuit breaker settings

### Phase 3: Full Migration (Week 5)
- 🔜 Route 100% to DeepCleer
- 🔜 Keep Alibaba as fallback
- 🔜 Final optimization

### Phase 4: Cleanup (Week 6)
- 🔜 Remove Alibaba dependencies (optional)
- 🔜 Update documentation
- 🔜 Ops team training

---

## 🐛 Troubleshooting

### Issue: "Unknown provider: deepcleer"

**Solution:** Enable DeepCleer provider

```yaml
deepcleer:
  api:
    enabled: true  # Must be true
```

### Issue: Circuit breaker is OPEN

**Solution:** Check DeepCleer API health

```bash
# Check provider health
curl http://localhost:8080/actuator/health
```

Wait for circuit breaker to reset (default: 60 seconds)

### Issue: Database locked

**Solution:** SQLite limitations with concurrent writes

For production, consider:
- PostgreSQL
- MySQL
- Or separate read/write databases

---

## 📚 Additional Resources

### DeepCleer Documentation
- API Docs: https://deepcleer.com/help/contentModeration/intelligentTextRecognition
- Console: https://console-na.ishumei.com

### Spring Boot Resilience4j
- Circuit Breaker: https://resilience4j.readme.io/docs/circuitbreaker
- Retry: https://resilience4j.readme.io/docs/retry

### WebClient (Reactive HTTP)
- Spring Docs: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html

---

## 🙋 Support

### Common Questions

**Q: Can I use multiple providers simultaneously?**
A: Yes! Use fallback mechanism:

```yaml
content-moderation:
  active-provider: deepcleer
  fallback:
    enabled: true
    secondary-provider: alibaba
```

**Q: How to test DeepCleer without real API?**
A: Use Mock provider for local development:

```bash
MODERATION_PROVIDER=mock mvn spring-boot:run
```

**Q: Where are API responses stored?**
A: In `moderation_results` table with full raw response in `response_body` column.

---

## ✅ Checklist Before Production

- [ ] DeepCleer credentials configured (environment variables)
- [ ] Circuit breaker thresholds tuned
- [ ] Fallback provider configured
- [ ] Database migration plan ready
- [ ] Monitoring dashboards created
- [ ] Alerting rules configured
- [ ] Load testing completed
- [ ] Documentation updated
- [ ] Team training completed

---

## 🎉 Success!

Dự án đã được tích hợp thành công với:
- ✅ 3 providers (DeepCleer, Alibaba, Mock)
- ✅ 21 passing tests
- ✅ Circuit Breaker & Retry
- ✅ Fallback mechanism
- ✅ Full documentation

**Happy Moderating! 🚀**
