#!/usr/bin/env python3
"""
Load Test Script for Content Moderation API
Supports configurable concurrency, total requests, and rate limiting
"""

import argparse
import concurrent.futures
import json
import random
import time
import uuid
from datetime import datetime
from typing import List, Dict
import requests
import sys

# Màu cho console output
class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

# Sample texts để test
SAMPLE_TEXTS = [
    "This is a normal comment about technology.",
    "I love this product, it's amazing!",
    "This service is terrible and should be banned.",
    "Maybe this is spam content?",
    "Hello world, this is a test message.",
    "Check out this great deal on illegal substances!",
    "The weather is nice today.",
    "This might be suspicious activity.",
    "Great customer service, highly recommended!",
    "Bad experience, very disappointed.",
]


class LoadTester:
    def __init__(self, base_url: str, concurrency: int, total_requests: int,
                 rate_limit: int, run_id: str):
        self.base_url = base_url
        self.concurrency = concurrency
        self.total_requests = total_requests
        self.rate_limit = rate_limit
        self.run_id = run_id
        self.results = []
        self.start_time = None
        self.end_time = None

    def generate_request(self, index: int) -> Dict:
        """Generate một request payload"""
        return {
            "id": f"req-{self.run_id}-{index}",
            "text": random.choice(SAMPLE_TEXTS),
            "runId": self.run_id
        }

    def send_request(self, payload: Dict) -> Dict:
        """Gửi request đến API và đo latency"""
        start = time.time()
        result = {
            "request_id": payload["id"],
            "success": False,
            "latency_ms": 0,
            "status_code": 0,
            "error": None
        }

        try:
            response = requests.post(
                f"{self.base_url}/api/v1/moderate",
                json=payload,
                timeout=30
            )

            latency = int((time.time() - start) * 1000)
            result["latency_ms"] = latency
            result["status_code"] = response.status_code
            result["success"] = response.status_code == 200

            if response.status_code == 200:
                result["response"] = response.json()
            else:
                result["error"] = f"HTTP {response.status_code}"

        except Exception as e:
            latency = int((time.time() - start) * 1000)
            result["latency_ms"] = latency
            result["error"] = str(e)
            result["success"] = False

        return result

    def run_test(self):
        """Chạy load test với concurrency đã cấu hình"""
        print(f"\n{Colors.HEADER}{'='*60}{Colors.ENDC}")
        print(f"{Colors.BOLD}Starting Load Test{Colors.ENDC}")
        print(f"{Colors.HEADER}{'='*60}{Colors.ENDC}\n")

        print(f"  Run ID:          {Colors.OKCYAN}{self.run_id}{Colors.ENDC}")
        print(f"  Target URL:      {self.base_url}")
        print(f"  Total Requests:  {self.total_requests}")
        print(f"  Concurrency:     {self.concurrency}")
        print(f"  Rate Limit:      {self.rate_limit if self.rate_limit > 0 else 'Unlimited'} req/s")
        print(f"\n{Colors.HEADER}{'='*60}{Colors.ENDC}\n")

        # Generate all requests
        requests_list = [self.generate_request(i) for i in range(self.total_requests)]

        self.start_time = time.time()
        completed = 0

        # Execute requests with concurrency
        with concurrent.futures.ThreadPoolExecutor(max_workers=self.concurrency) as executor:
            futures = []

            for request_payload in requests_list:
                # Rate limiting
                if self.rate_limit > 0:
                    time.sleep(1.0 / self.rate_limit)

                future = executor.submit(self.send_request, request_payload)
                futures.append(future)

            # Collect results
            for future in concurrent.futures.as_completed(futures):
                result = future.result()
                self.results.append(result)
                completed += 1

                # Progress indicator
                if completed % max(1, self.total_requests // 20) == 0:
                    progress = (completed / self.total_requests) * 100
                    print(f"Progress: {progress:.1f}% ({completed}/{self.total_requests})", end='\r')

        self.end_time = time.time()
        print(f"\nProgress: 100.0% ({self.total_requests}/{self.total_requests})")
        print(f"\n{Colors.OKGREEN}Test completed!{Colors.ENDC}\n")

    def calculate_metrics(self) -> Dict:
        """Tính toán metrics từ results"""
        if not self.results:
            return {}

        latencies = sorted([r["latency_ms"] for r in self.results])
        success_count = sum(1 for r in self.results if r["success"])
        fail_count = len(self.results) - success_count

        duration = self.end_time - self.start_time
        throughput = self.total_requests / duration if duration > 0 else 0

        def percentile(data, p):
            index = int(len(data) * p / 100)
            return data[min(index, len(data) - 1)]

        return {
            "run_id": self.run_id,
            "total_requests": self.total_requests,
            "success_count": success_count,
            "fail_count": fail_count,
            "success_rate": (success_count / self.total_requests * 100) if self.total_requests > 0 else 0,
            "min_latency": min(latencies) if latencies else 0,
            "max_latency": max(latencies) if latencies else 0,
            "avg_latency": sum(latencies) / len(latencies) if latencies else 0,
            "p50_latency": percentile(latencies, 50),
            "p95_latency": percentile(latencies, 95),
            "p99_latency": percentile(latencies, 99),
            "throughput_rps": throughput,
            "duration_seconds": duration,
            "concurrency": self.concurrency
        }

    def print_summary(self, metrics: Dict):
        """In tóm tắt kết quả"""
        print(f"\n{Colors.HEADER}{'='*60}{Colors.ENDC}")
        print(f"{Colors.BOLD}Test Results Summary{Colors.ENDC}")
        print(f"{Colors.HEADER}{'='*60}{Colors.ENDC}\n")

        success_color = Colors.OKGREEN if metrics["success_rate"] >= 95 else Colors.WARNING

        print(f"  Run ID:           {metrics['run_id']}")
        print(f"  Duration:         {metrics['duration_seconds']:.2f}s")
        print(f"  Total Requests:   {metrics['total_requests']}")
        print(f"  Success:          {success_color}{metrics['success_count']}{Colors.ENDC}")
        print(f"  Failed:           {Colors.FAIL if metrics['fail_count'] > 0 else Colors.ENDC}{metrics['fail_count']}{Colors.ENDC}")
        print(f"  Success Rate:     {success_color}{metrics['success_rate']:.2f}%{Colors.ENDC}")
        print(f"\n{Colors.BOLD}Latency (ms):{Colors.ENDC}")
        print(f"  Min:              {metrics['min_latency']}")
        print(f"  Max:              {metrics['max_latency']}")
        print(f"  Average:          {metrics['avg_latency']:.2f}")
        print(f"  P50:              {metrics['p50_latency']}")
        print(f"  P95:              {metrics['p95_latency']}")
        print(f"  P99:              {metrics['p99_latency']}")
        print(f"\n{Colors.BOLD}Throughput:{Colors.ENDC}")
        print(f"  Requests/sec:     {Colors.OKGREEN}{metrics['throughput_rps']:.2f}{Colors.ENDC}")
        print(f"\n{Colors.HEADER}{'='*60}{Colors.ENDC}\n")

    def save_metrics_to_api(self, metrics: Dict):
        """Lưu metrics vào API endpoint"""
        try:
            url = f"{self.base_url}/api/v1/metrics/calculate/{self.run_id}"
            params = {"concurrency": self.concurrency}
            response = requests.post(url, params=params, timeout=10)

            if response.status_code == 200:
                print(f"{Colors.OKGREEN}✓ Metrics saved to database via API{Colors.ENDC}")
            else:
                print(f"{Colors.WARNING}⚠ Failed to save metrics: HTTP {response.status_code}{Colors.ENDC}")
        except Exception as e:
            print(f"{Colors.WARNING}⚠ Failed to save metrics: {str(e)}{Colors.ENDC}")


def main():
    parser = argparse.ArgumentParser(
        description="Load test script for Content Moderation API",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Quick test: 10 requests, 2 concurrent
  python load_test.py --requests 10 --concurrency 2

  # Medium test: 100 requests, 20 concurrent
  python load_test.py --requests 100 --concurrency 20

  # Stress test: 1000 requests, 200 concurrent
  python load_test.py --requests 1000 --concurrency 200

  # Rate limited: 50 req/s max
  python load_test.py --requests 500 --concurrency 50 --rate-limit 50

  # Custom endpoint
  python load_test.py --url http://localhost:8080 --requests 100
        """
    )

    parser.add_argument("--url", default="http://localhost:8080",
                        help="Base URL của API (default: http://localhost:8080)")
    parser.add_argument("--requests", type=int, default=10,
                        help="Tổng số requests (default: 10)")
    parser.add_argument("--concurrency", type=int, default=2,
                        help="Số requests đồng thời (default: 2)")
    parser.add_argument("--rate-limit", type=int, default=0,
                        help="Rate limit (requests/second), 0 = unlimited (default: 0)")
    parser.add_argument("--run-id", default=None,
                        help="Run ID tùy chỉnh (default: auto-generated)")

    args = parser.parse_args()

    # Generate run ID
    run_id = args.run_id or f"run-{datetime.now().strftime('%Y%m%d-%H%M%S')}-{uuid.uuid4().hex[:8]}"

    # Create and run tester
    tester = LoadTester(
        base_url=args.url,
        concurrency=args.concurrency,
        total_requests=args.requests,
        rate_limit=args.rate_limit,
        run_id=run_id
    )

    # Run test
    tester.run_test()

    # Calculate and display metrics
    metrics = tester.calculate_metrics()
    tester.print_summary(metrics)

    # Save to API
    tester.save_metrics_to_api(metrics)

    print(f"\n{Colors.OKCYAN}To view detailed report:{Colors.ENDC}")
    print(f"  curl {args.url}/api/v1/metrics/report/{run_id}\n")


if __name__ == "__main__":
    main()
