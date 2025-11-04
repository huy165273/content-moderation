# 📘 Hướng Dẫn Exception Handling - Content Moderation API

## 📋 Tổng Quan

Project này đã được tích hợp **exception handling chuẩn** để đảm bảo:

✅ Bắt và xử lý **mọi loại exception** có thể xảy ra khi gọi API
✅ Trả về response có **format JSON đồng nhất, rõ ràng, dễ debug**
✅ API luôn phản hồi đúng chuẩn, ngay cả khi dữ liệu truyền vào sai, thiếu hoặc lỗi logic
✅ **Không leak thông tin nhạy cảm** (stack trace, DB errors) ra ngoài
✅ **Log chi tiết lỗi** phục vụ debugging

---

## 📦 Các File Được Thêm/Chỉnh Sửa

### 1. **Custom Exception Classes** (Package: `exception`)

| File | Đường dẫn | Mô tả |
|------|-----------|-------|
| `EntityNotFoundException.java` | `src/main/java/com/example/moderation/exception/` | Exception khi không tìm thấy entity (404) |
| `BusinessException.java` | `src/main/java/com/example/moderation/exception/` | Exception cho lỗi business logic (422) |
| `ExternalApiException.java` | `src/main/java/com/example/moderation/exception/` | Exception khi external API (Alibaba Cloud) lỗi (502) |

### 2. **Error Response Model**

| File | Đường dẫn | Mô tả |
|------|-----------|-------|
| `ApiError.java` | `src/main/java/com/example/moderation/exception/` | Class model chuẩn cho error response JSON |

### 3. **Global Exception Handler**

| File | Đường dẫn | Mô tả |
|------|-----------|-------|
| `GlobalExceptionHandler.java` | `src/main/java/com/example/moderation/exception/` | Xử lý tập trung tất cả exceptions với `@RestControllerAdvice` |

### 4. **Controllers (Đã Update)**

| File | Thay đổi |
|------|----------|
| `ModerationController.java` | + Thêm `@Validated`<br>+ Thêm validation cho `concurrency` parameter (`@Min`, `@Max`) |
| `MetricsController.java` | + Thêm `@Validated`<br>+ Throw `EntityNotFoundException` thay vì return `notFound()`<br>+ Thêm validation cho `concurrency` parameter |

---

## 🎯 Format JSON Response Chuẩn

### **Single Error Message**

```json
{
  "timestamp": "2025-11-04T17:02:10Z",
  "status": 400,
  "error": "Bad Request",
  "message": "JSON request không hợp lệ hoặc sai định dạng",
  "path": "/api/v1/moderate"
}
```

### **Multiple Error Messages (Validation)**

```json
{
  "timestamp": "2025-11-04T17:02:10Z",
  "status": 400,
  "error": "Validation Failed",
  "messages": [
    "id: ID không được để trống",
    "text: Text không được để trống"
  ],
  "path": "/api/v1/moderate"
}
```

### **Error với Custom Error Code**

```json
{
  "timestamp": "2025-11-04T17:02:10Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Concurrency vượt quá giới hạn cho phép",
  "path": "/api/v1/moderate/batch",
  "errorCode": "CONCURRENCY_EXCEEDED"
}
```

---

## 🛡️ Danh Sách Exception Được Xử Lý

| Exception | HTTP Status | Khi Nào Xảy Ra | Error Response |
|-----------|------------|-----------------|----------------|
| `MethodArgumentNotValidException` | 400 Bad Request | `@Valid` validation fail (DTO fields) | Multiple error messages |
| `ConstraintViolationException` | 400 Bad Request | Constraint validation fail (parameters, path variables) | Multiple constraint messages |
| `HttpMessageNotReadableException` | 400 Bad Request | JSON body malformed, sai format | "JSON request không hợp lệ hoặc sai định dạng" |
| `MissingServletRequestParameterException` | 400 Bad Request | Thiếu required request parameter | "Thiếu parameter bắt buộc: 'concurrency' (kiểu int)" |
| `MethodArgumentTypeMismatchException` | 400 Bad Request | Parameter type không khớp (vd: string thay vì int) | "Parameter 'concurrency' có giá trị 'abc' không hợp lệ" |
| `HttpRequestMethodNotSupportedException` | 405 Method Not Allowed | Gọi sai HTTP method (GET thay vì POST) | "HTTP method 'GET' không được hỗ trợ cho endpoint này" |
| `EntityNotFoundException` | 404 Not Found | Không tìm thấy entity (TestRun, ...) | "TestRun không tồn tại với ID: xyz" |
| `IllegalArgumentException` | 400 Bad Request | Argument không hợp lệ trong logic | Exception message |
| `BusinessException` | 422 Unprocessable Entity | Business logic error | Custom business error message |
| `ExternalApiException` | 502 Bad Gateway | External API (Alibaba Cloud) lỗi | "Lỗi khi gọi API Alibaba: ..." |
| `RuntimeException` | 500 Internal Server Error | Runtime error không xác định | "Đã xảy ra lỗi trong quá trình xử lý" |
| `Exception` (catch-all) | 500 Internal Server Error | Bất kỳ exception nào khác | "Internal Server Error. Vui lòng liên hệ administrator." |

---

## 📚 Ví Dụ Request/Response Thực Tế

### **1. Validation Error: Thiếu Field Bắt Buộc**

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Test content"
  }'
```

**Response: 400 Bad Request**
```json
{
  "timestamp": "2025-11-04T10:30:15Z",
  "status": 400,
  "error": "Validation Failed",
  "messages": [
    "id: ID không được để trống"
  ],
  "path": "/api/v1/moderate"
}
```

---

### **2. Validation Error: Multiple Fields**

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{
    "id": "",
    "text": ""
  }'
```

**Response: 400 Bad Request**
```json
{
  "timestamp": "2025-11-04T10:31:22Z",
  "status": 400,
  "error": "Validation Failed",
  "messages": [
    "id: ID không được để trống",
    "text: Text không được để trống"
  ],
  "path": "/api/v1/moderate"
}
```

---

### **3. Malformed JSON**

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-123"
    "text": "Missing comma"
  }'
```

**Response: 400 Bad Request**
```json
{
  "timestamp": "2025-11-04T10:32:45Z",
  "status": 400,
  "error": "Bad Request",
  "message": "JSON request không hợp lệ hoặc sai định dạng",
  "path": "/api/v1/moderate"
}
```

---

### **4. Parameter Validation Error: Concurrency Out of Range**

**Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/moderate/batch?concurrency=999" \
  -H "Content-Type: application/json" \
  -d '[
    {"id": "1", "text": "Test"}
  ]'
```

**Response: 400 Bad Request**
```json
{
  "timestamp": "2025-11-04T10:33:18Z",
  "status": 400,
  "error": "Bad Request",
  "messages": [
    "Concurrency không được vượt quá 500"
  ],
  "path": "/api/v1/moderate/batch"
}
```

---

### **5. Parameter Type Mismatch**

**Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/moderate/batch?concurrency=abc" \
  -H "Content-Type: application/json" \
  -d '[
    {"id": "1", "text": "Test"}
  ]'
```

**Response: 400 Bad Request**
```json
{
  "timestamp": "2025-11-04T10:34:50Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Parameter 'concurrency' có giá trị 'abc' không hợp lệ. Cần kiểu dữ liệu: int",
  "path": "/api/v1/moderate/batch"
}
```

---

### **6. HTTP Method Not Allowed**

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json"
```

**Response: 405 Method Not Allowed**
```json
{
  "timestamp": "2025-11-04T10:35:30Z",
  "status": 405,
  "error": "Method Not Allowed",
  "message": "HTTP method 'GET' không được hỗ trợ cho endpoint này. Các method được hỗ trợ: [POST]",
  "path": "/api/v1/moderate"
}
```

---

### **7. Entity Not Found**

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/metrics/report/non-existent-run-id
```

**Response: 404 Not Found**
```json
{
  "timestamp": "2025-11-04T10:36:12Z",
  "status": 404,
  "error": "Not Found",
  "message": "TestRun không tồn tại với ID: non-existent-run-id",
  "path": "/api/v1/metrics/report/non-existent-run-id"
}
```

---

### **8. Internal Server Error (Generic)**

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-999",
    "text": "Test content that triggers internal error"
  }'
```

**Response: 500 Internal Server Error**
```json
{
  "timestamp": "2025-11-04T10:37:45Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Đã xảy ra lỗi trong quá trình xử lý. Vui lòng thử lại sau.",
  "path": "/api/v1/moderate"
}
```

> ⚠️ **Lưu ý:** Chi tiết lỗi (stack trace) được log trong server logs nhưng **không trả về cho client** để bảo mật.

---

## 🧪 Hướng Dẫn Test Exception Handling

### **1. Build và Run Application**

```bash
# Build project
mvn clean install

# Run application (Mock mode)
mvn spring-boot:run
```

### **2. Test Validation Errors**

**Test 1: Thiếu required field**
```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{"text": "Test"}' \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected:** HTTP 400 với message "id: ID không được để trống"

**Test 2: Blank fields**
```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{"id": "", "text": ""}' \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected:** HTTP 400 với multiple validation messages

### **3. Test Parameter Validation**

**Test 1: Concurrency < 1**
```bash
curl -X POST "http://localhost:8080/api/v1/moderate/batch?concurrency=0" \
  -H "Content-Type: application/json" \
  -d '[{"id": "1", "text": "Test"}]' \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected:** HTTP 400 với message "Concurrency phải >= 1"

**Test 2: Concurrency > 500**
```bash
curl -X POST "http://localhost:8080/api/v1/moderate/batch?concurrency=600" \
  -H "Content-Type: application/json" \
  -d '[{"id": "1", "text": "Test"}]' \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected:** HTTP 400 với message "Concurrency không được vượt quá 500"

### **4. Test Malformed JSON**

```bash
curl -X POST http://localhost:8080/api/v1/moderate \
  -H "Content-Type: application/json" \
  -d '{"id": "test" "text": "Missing comma"}' \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected:** HTTP 400 với message "JSON request không hợp lệ"

### **5. Test Wrong HTTP Method**

```bash
curl -X GET http://localhost:8080/api/v1/moderate \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected:** HTTP 405 với message về method not allowed

### **6. Test Entity Not Found**

```bash
curl -X GET http://localhost:8080/api/v1/metrics/report/invalid-id-12345 \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected:** HTTP 404 với message "TestRun không tồn tại với ID: invalid-id-12345"

### **7. Test Parameter Type Mismatch**

```bash
curl -X POST "http://localhost:8080/api/v1/moderate/batch?concurrency=abc" \
  -H "Content-Type: application/json" \
  -d '[{"id": "1", "text": "Test"}]' \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected:** HTTP 400 với message về type mismatch

---

## 🔍 Kiểm Tra Logs

Khi exception xảy ra, logs sẽ được ghi chi tiết trong console:

**Validation Error Log:**
```
2025-11-04 10:30:15.123 [http-nio-8080-exec-1] WARN  c.e.m.e.GlobalExceptionHandler - Validation failed for request /api/v1/moderate: [id: ID không được để trống]
```

**Entity Not Found Log:**
```
2025-11-04 10:36:12.456 [http-nio-8080-exec-2] WARN  c.e.m.e.GlobalExceptionHandler - Entity not found at /api/v1/metrics/report/non-existent-run-id: TestRun không tồn tại với ID: non-existent-run-id
```

**Internal Error Log (với stack trace):**
```
2025-11-04 10:37:45.789 [http-nio-8080-exec-3] ERROR c.e.m.e.GlobalExceptionHandler - Runtime error at /api/v1/moderate: NullPointerException
java.lang.NullPointerException: Cannot invoke "String.length()" because "text" is null
    at com.example.moderation.service.ContentModerationService.moderateContent(ContentModerationService.java:42)
    ...
```

---

## ✅ Checklist: Đảm Bảo Exception Handling Hoàn Chỉnh

### **A. Exception Handling Infrastructure**

- [x] **GlobalExceptionHandler.java** với `@RestControllerAdvice` đã được tạo
- [x] **ApiError.java** model chuẩn cho error response đã được tạo
- [x] Custom exceptions được định nghĩa:
  - [x] `EntityNotFoundException` (404)
  - [x] `BusinessException` (422)
  - [x] `ExternalApiException` (502)

### **B. Exception Types Được Handle**

- [x] `MethodArgumentNotValidException` - Validation errors (@Valid)
- [x] `ConstraintViolationException` - Constraint violations
- [x] `HttpMessageNotReadableException` - Malformed JSON
- [x] `MissingServletRequestParameterException` - Missing parameters
- [x] `MethodArgumentTypeMismatchException` - Type mismatch
- [x] `HttpRequestMethodNotSupportedException` - Wrong HTTP method
- [x] `EntityNotFoundException` - Entity not found (404)
- [x] `BusinessException` - Business logic errors (422)
- [x] `ExternalApiException` - External API errors (502)
- [x] `IllegalArgumentException` - Invalid arguments
- [x] `RuntimeException` - Runtime errors (500)
- [x] `Exception` - Catch-all for unexpected errors (500)

### **C. Controllers và Validation**

- [x] `ModerationController`:
  - [x] Thêm annotation `@Validated`
  - [x] Validation cho `concurrency` parameter (`@Min`, `@Max`)
  - [x] `@Valid` cho request body
- [x] `MetricsController`:
  - [x] Thêm annotation `@Validated`
  - [x] Throw `EntityNotFoundException` thay vì return `notFound()`
  - [x] Validation cho `concurrency` parameter

### **D. DTOs và Entities**

- [x] `ModerationRequest`:
  - [x] `@NotBlank` cho `id` field
  - [x] `@NotBlank` cho `text` field

### **E. Response Format**

- [x] JSON format đồng nhất cho tất cả errors
- [x] Timestamp (ISO-8601 UTC)
- [x] HTTP status code
- [x] Error type/name
- [x] Clear error message(s)
- [x] Request path
- [x] Support cho multiple validation messages

### **F. Security & Best Practices**

- [x] **Không leak stack trace** ra ngoài (500 errors)
- [x] **Không leak database errors** ra ngoài
- [x] **Log chi tiết** cho debugging (server-side only)
- [x] **Generic messages** cho 500 errors
- [x] **Consistent error structure** across all endpoints
- [x] **Proper HTTP status codes** cho từng loại lỗi

### **G. Dependencies**

- [x] `spring-boot-starter-validation` đã có trong `pom.xml`
- [x] `spring-boot-starter-web` đã có trong `pom.xml`
- [x] Không cần thêm dependency nào

### **H. Testing**

- [x] Test cases cho validation errors
- [x] Test cases cho malformed JSON
- [x] Test cases cho parameter validation
- [x] Test cases cho entity not found
- [x] Test cases cho wrong HTTP method
- [x] Test cases cho type mismatch
- [x] Verify logs được ghi đúng
- [x] Verify response format chuẩn

---

## 📊 So Sánh: Trước và Sau Khi Có Exception Handling

### **Trước (Without Global Exception Handler)**

❌ Return `ResponseEntity.notFound()` từ controller
❌ Exception không được handle thống nhất
❌ Error format không đồng nhất
❌ Thiếu timestamp, path thông tin
❌ Validation errors không rõ ràng
❌ Stack trace có thể leak ra ngoài

**Ví dụ response trước:**
```json
// EntityNotFoundException: Chỉ trả về empty body
{}
// HTTP Status: 404
```

### **Sau (With Global Exception Handler)**

✅ Throw exception từ service/controller, GlobalExceptionHandler tự động xử lý
✅ Tất cả exceptions được handle tập trung
✅ Error format JSON đồng nhất, chuẩn REST
✅ Có đầy đủ timestamp, status, error, message, path
✅ Validation errors rõ ràng, chi tiết từng field
✅ Bảo mật: không leak thông tin nhạy cảm

**Ví dụ response sau:**
```json
{
  "timestamp": "2025-11-04T10:36:12Z",
  "status": 404,
  "error": "Not Found",
  "message": "TestRun không tồn tại với ID: xyz",
  "path": "/api/v1/metrics/report/xyz"
}
```

---

## 🎓 Best Practices Được Áp Dụng

### **1. Centralized Exception Handling**
- Sử dụng `@RestControllerAdvice` để xử lý tập trung
- Tránh try-catch rải rác trong controllers
- Dễ maintain và extend

### **2. Consistent Error Response**
- Tất cả errors đều trả về `ApiError` model
- Format JSON đồng nhất
- Dễ dàng parse và xử lý ở client

### **3. Proper HTTP Status Codes**
- 400 Bad Request: Validation, malformed input
- 404 Not Found: Entity không tồn tại
- 405 Method Not Allowed: Sai HTTP method
- 422 Unprocessable Entity: Business logic errors
- 500 Internal Server Error: Server-side errors
- 502 Bad Gateway: External API errors

### **4. Security First**
- Generic messages cho 500 errors
- Không expose stack traces
- Log chi tiết chỉ server-side
- Không leak database errors

### **5. Developer Friendly**
- Clear, descriptive error messages
- Multiple validation errors trong 1 response
- Request path để dễ debug
- Timestamp để track issues

### **6. Validation Best Practices**
- `@Validated` ở class level
- `@Valid` cho request body
- `@Min`, `@Max` cho parameters
- `@NotBlank`, `@NotNull` cho DTOs

---

## 🚀 Tích Hợp Với Existing Code

### **Không Ảnh Hưởng Đến Code Hiện Tại**

✅ Service layer (`ContentModerationService`, `MetricsService`) **không cần thay đổi**
✅ DTOs hiện tại đã có validation annotations
✅ Repositories không bị ảnh hưởng
✅ Existing endpoints vẫn hoạt động bình thường

### **Chỉ Cần**

1. Controllers throw exceptions thay vì return error responses
2. GlobalExceptionHandler tự động intercept và xử lý
3. Trả về ApiError format chuẩn

---

## 📞 Liên Hệ & Support

Nếu có vấn đề hoặc câu hỏi về exception handling:

1. **Check logs**: Xem chi tiết exception trong console/log files
2. **Verify request**: Kiểm tra JSON format, parameters, HTTP method
3. **Test với curl**: Sử dụng các ví dụ trong README này
4. **Review code**: Xem `GlobalExceptionHandler.java` để hiểu logic xử lý

---

## 📝 Ghi Chú Quan Trọng

⚠️ **Lưu ý khi deploy production:**

1. Đảm bảo `logging.level.com.example.moderation` không để `DEBUG` trong production
2. Cân nhắc thêm request ID/correlation ID để tracking
3. Tích hợp với monitoring tools (Prometheus, Grafana)
4. Set up alerting cho 500 errors
5. Review logs thường xuyên để phát hiện patterns
6. Cân nhắc thêm rate limiting để tránh abuse

---

**🎉 Exception handling đã sẵn sàng! API của bạn giờ đây luôn trả về response chuẩn, dễ debug và user-friendly.**
