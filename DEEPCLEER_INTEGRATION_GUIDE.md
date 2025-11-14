# DeepCleer Integration Guide

## Overview

This document describes the correct integration with DeepCleer Content Moderation API according to the official documentation.

## API Configuration

### Endpoint Information

**Base URL Options (by cluster):**
- Beijing: `http://api-text-bj.fengkongcloud.com`
- Shanghai: `http://api-text-sh.fengkongcloud.com`
- USA: `http://api-text-fjny.fengkongcloud.com`
- Singapore: `http://api-text-xjp.fengkongcloud.com`

**API Path:** `/text/v4`

**Full URL Example:** `http://api-text-bj.fengkongcloud.com/text/v4`

### Required Configuration

Set these environment variables:
```bash
export DEEPCLEER_ACCESS_KEY=your_access_key
export DEEPCLEER_APP_ID=your_app_id
export DEEPCLEER_ENABLED=true
```

Or configure in `application.yml`:
```yaml
deepcleer:
  api:
    access-key: ${DEEPCLEER_ACCESS_KEY}
    app-id: ${DEEPCLEER_APP_ID}
    event-id: text
    base-url: http://api-text-bj.fengkongcloud.com
    text-moderation-endpoint: /text/v4
    enabled: true
```

## Request Format

### Required Fields

```json
{
  "accessKey": "your_access_key",
  "appId": "your_app_id",
  "eventId": "text",
  "type": "TEXTRISK",
  "data": {
    "text": "Content to moderate"
  }
}
```

### Complete Request Example

```json
{
  "accessKey": "*************",
  "appId": "default",
  "eventId": "text",
  "type": "TEXTRISK",
  "data": {
    "text": "Add me on QQ: qq12345",
    "tokenId": "4567898765jhgfdsa",
    "ip": "118.89.214.89",
    "deviceId": "*************",
    "nickname": "***********",
    "extra": {
      "topic": "12345",
      "atId": "username1",
      "room": "ceshi123",
      "receiveTokenId": "username2"
    }
  }
}
```

### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `accessKey` | string | Yes | Authentication key from DeepCleer |
| `appId` | string | Yes | Application identifier |
| `eventId` | string | Yes | Event identifier (e.g., "text") |
| `type` | string | Yes | Risk detection type (e.g., "TEXTRISK", "FRAUD", "TEXTMINOR") |
| `data.text` | string | Yes | Text content to moderate (max 1MB) |
| `data.tokenId` | string | No | User identifier |
| `data.ip` | string | No | User IP address |
| `data.deviceId` | string | No | Device identifier |
| `data.nickname` | string | No | User nickname |
| `data.extra` | object | No | Additional metadata |

## Response Format

### Success Response (code = 1100)

```json
{
  "code": 1100,
  "message": "Success",
  "requestId": "bb917ec5fa11fd02d226fb384968feb1",
  "riskLevel": "REJECT",
  "riskLabel1": "ad",
  "riskLabel2": "contact_info",
  "riskLabel3": "contact_info",
  "riskDescription": "Advertising: Contact information: Contact information",
  "riskDetail": {},
  "tokenLabels": {},
  "auxInfo": {
    "contactResult": [
      {
        "contactString": "qq12345",
        "contactType": 2
      }
    ],
    "filteredText": "Add me on QQ: **12345"
  },
  "allLabels": [
    {
      "probability": 1,
      "riskDescription": "Advertising: Contact information: Contact information",
      "riskDetail": {},
      "riskLabel1": "ad",
      "riskLabel2": "contact_info",
      "riskLabel3": "contact_info",
      "riskLevel": "REJECT"
    }
  ],
  "businessLabels": [],
  "finalResult": 1,
  "resultType": 0
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `code` | int | Return code (1100 = success) |
| `message` | string | Description of return code |
| `requestId` | string | Unique request identifier |
| `riskLevel` | string | Disposal recommendation: PASS, REVIEW, REJECT |
| `riskLabel1` | string | Primary risk label |
| `riskLabel2` | string | Secondary risk label |
| `riskLabel3` | string | Tertiary risk label |
| `riskDescription` | string | Risk reason description |
| `riskDetail` | object | Mapped risk details |
| `auxInfo` | object | Auxiliary information |
| `allLabels` | array | All matched risk labels |
| `businessLabels` | array | Business labels |
| `finalResult` | int | 1 = final result, 0 = needs manual review |
| `resultType` | int | 0 = machine review, 1 = human review |

### Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1100 | Success | Request processed successfully |
| 1901 | QPS limit exceeded | Rate limit reached |
| 1902 | Invalid parameters | Request parameters are invalid |
| 1903 | Service failure | Internal service error |
| 1905 | Character limit exceeded | Text too long (max 1MB) |
| 9101 | Unauthorized operation | Invalid credentials |

## Risk Labels (Chinese Content)

When `lang` is "zh" or auto-detected as Chinese:

| Primary Label | Identifier | Type | Description |
|--------------|------------|------|-------------|
| Political | politics | TEXTRISK | Political content |
| Violence | violence | TEXTRISK | Violent content |
| Pornography | porn | TEXTRISK | Pornographic content |
| Banned | ban | TEXTRISK | Banned content |
| Abuse | abuse | TEXTRISK | Abusive language |
| Advertising Law | ad_law | TEXTRISK | Violates advertising law |
| Advertising | ad | TEXTRISK | Advertising content |
| Blacklist | blacklist | TEXTRISK | Blacklisted content |
| Meaningless | meaningless | TEXTRISK | Spam/meaningless |
| Privacy | privacy | TEXTRISK | Privacy violation |
| Fraud | fraud | FRAUD | Fraudulent content |
| Minor | minor | TEXTMINOR | Minor-related content |

## Risk Labels (International Content)

For non-Chinese content:

| Primary Label | Identifier | Type | Description |
|--------------|------------|------|-------------|
| Political | Politics | TEXTRISK | Political content |
| Violence | Violence | TEXTRISK | Violent content |
| Pornography | Erotic | TEXTRISK | Pornographic content |
| Banned | Prohibit | TEXTRISK | Banned content |
| Abuse | Abuse | TEXTRISK | Abusive language |
| Advertising | Ads | TEXTRISK | Advertising content |
| Blacklist | Blacklist | TEXTRISK | Blacklisted content |

## Testing the Integration

### 1. Run Unit Tests

```bash
# Run all DeepCleer tests
mvn test -Dtest=DeepCleerProviderTest

# Run specific test
mvn test -Dtest=DeepCleerProviderTest#testModerateRiskyText
```

### 2. Manual API Test

Create a test file `test-deepcleer.sh`:

```bash
#!/bin/bash

curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Add me on QQ: qq12345",
    "provider": "deepcleer",
    "options": {
      "userId": "test_user",
      "ip": "118.89.214.89"
    }
  }'
```

### 3. Expected Test Results

#### Clean Text Test
Input: "Hello, this is a normal message."

Expected Response:
```json
{
  "riskLevel": "PASS",
  "riskLabel1": "normal",
  "riskLabel2": "",
  "riskLabel3": "",
  "riskDescription": "normal"
}
```

Mapped to internal format:
```json
{
  "providerName": "deepcleer",
  "riskLevel": "LOW",
  "labels": ["normal"],
  "confidenceScore": null
}
```

#### Risky Text Test
Input: "Add me on QQ: qq12345"

Expected Response:
```json
{
  "riskLevel": "REJECT",
  "riskLabel1": "ad",
  "riskLabel2": "contact_info",
  "riskLabel3": "contact_info",
  "riskDescription": "Advertising: Contact information: Contact information"
}
```

Mapped to internal format:
```json
{
  "providerName": "deepcleer",
  "riskLevel": "HIGH",
  "labels": ["ad", "contact_info", "contact_info"],
  "confidenceScore": 1.0
}
```

## Risk Level Mapping

DeepCleer uses three risk levels that are mapped to our internal system:

| DeepCleer Level | Internal Level | Description |
|----------------|----------------|-------------|
| PASS | LOW | Content is safe, recommend direct approval |
| REVIEW | MEDIUM | Content is suspicious, recommend manual review |
| REJECT | HIGH | Content violates policy, recommend blocking |

## Troubleshooting

### Common Issues

#### 1. "Unauthorized operation" (code 9101)
**Cause:** Invalid `accessKey` or `appId`
**Solution:** Verify your credentials in the activation email from DeepCleer

#### 2. "Invalid parameters" (code 1902)
**Cause:** Missing required fields or incorrect format
**Solution:** Check that `accessKey`, `appId`, `eventId`, `type`, and `data.text` are all provided

#### 3. Connection timeout
**Cause:** Wrong base URL or network issues
**Solution:**
- Verify base URL is `http://api-text-bj.fengkongcloud.com` (or appropriate cluster)
- Check that endpoint is `/text/v4`
- Ensure firewall allows outbound HTTP traffic

#### 4. QPS limit exceeded (code 1901)
**Cause:** Too many requests per second
**Solution:** Implement rate limiting or contact DeepCleer to increase your quota

### Debug Logging

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.example.moderation.provider.deepcleer: DEBUG
```

This will log:
- Request URL and payload
- Response status and body
- Latency measurements
- Circuit breaker state changes

## Performance Recommendations

1. **Timeout Settings:**
   - DeepCleer recommends 1s timeout
   - Current setting: 6s read timeout (conservative)
   - Adjust based on your P95 latency

2. **Retry Configuration:**
   - Max attempts: 3
   - Backoff delay: 1s
   - Backoff multiplier: 2.0
   - Total max retry time: ~7s

3. **Circuit Breaker:**
   - Failure rate threshold: 50%
   - Wait duration: 60s
   - Sliding window: 10 requests

4. **Rate Limiting:**
   - Check your QPS quota with DeepCleer
   - Implement client-side rate limiting if needed

## Next Steps

1. ✅ Verify credentials are correct
2. ✅ Run unit tests: `mvn test -Dtest=DeepCleerProviderTest`
3. ✅ Test with sample data from documentation
4. ✅ Monitor response times and error rates
5. ✅ Set up alerts for high error rates
6. ✅ Configure appropriate timeouts for your use case

## Support

For issues with the DeepCleer API itself:
- Contact DeepCleer support with your `requestId`
- Check API status page
- Review activation email for correct credentials

For issues with this integration:
- Check logs in `logs/application.log`
- Enable debug logging
- Run unit tests to isolate the issue
