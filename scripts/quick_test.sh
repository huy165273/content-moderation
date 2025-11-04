#!/bin/bash
# Quick test script để test API nhanh

BASE_URL="${1:-http://localhost:8080}"

echo "=================================="
echo "Quick API Test"
echo "=================================="
echo "Base URL: $BASE_URL"
echo ""

# Test health endpoint
echo "1. Testing health endpoint..."
curl -s "$BASE_URL/api/v1/health"
echo -e "\n"

# Test single moderation request
echo "2. Testing single moderation request..."
curl -s -X POST "$BASE_URL/api/v1/moderate" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-001",
    "text": "This is a test message",
    "runId": "quick-test"
  }' | python -m json.tool
echo -e "\n"

# Test với suspicious content
echo "3. Testing with suspicious content..."
curl -s -X POST "$BASE_URL/api/v1/moderate" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-002",
    "text": "This is spam and illegal content!",
    "runId": "quick-test"
  }' | python -m json.tool
echo -e "\n"

# Get metrics
echo "4. Getting current metrics..."
curl -s "$BASE_URL/api/v1/metrics/current" | python -m json.tool
echo -e "\n"

echo "=================================="
echo "Test completed!"
echo "=================================="
