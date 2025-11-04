package com.example.moderation.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Cấu trúc chuẩn cho error response trong toàn bộ API.
 * Đảm bảo format JSON đồng nhất, dễ debug và rõ ràng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Không hiển thị field null trong JSON
public class ApiError {

    /**
     * Thời điểm xảy ra lỗi (ISO-8601 format).
     * Ví dụ: "2025-11-04T17:02:10Z"
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private String timestamp;

    /**
     * HTTP status code.
     * Ví dụ: 400, 404, 500
     */
    private Integer status;

    /**
     * Tên lỗi (HTTP status text).
     * Ví dụ: "Bad Request", "Not Found", "Internal Server Error"
     */
    private String error;

    /**
     * Thông điệp lỗi chi tiết cho single error.
     * Ví dụ: "Trường 'email' không hợp lệ"
     */
    private String message;

    /**
     * Danh sách thông điệp lỗi cho validation errors (multiple fields).
     * Chỉ hiển thị khi có nhiều lỗi validation.
     * Ví dụ: ["username không được để trống", "email sai định dạng"]
     */
    private List<String> messages;

    /**
     * Request path gây ra lỗi.
     * Ví dụ: "/api/v1/moderate"
     */
    private String path;

    /**
     * Error code tùy chỉnh (optional).
     * Dùng cho business logic errors.
     */
    private String errorCode;

    /**
     * Constructor cho single error message.
     *
     * @param status HTTP status
     * @param message Thông điệp lỗi
     * @param path Request path
     */
    public ApiError(HttpStatus status, String message, String path) {
        this.timestamp = getCurrentTimestamp();
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
        this.path = path;
    }

    /**
     * Constructor cho multiple error messages (validation).
     *
     * @param status HTTP status
     * @param messages Danh sách thông điệp lỗi
     * @param path Request path
     */
    public ApiError(HttpStatus status, List<String> messages, String path) {
        this.timestamp = getCurrentTimestamp();
        this.status = status.value();
        this.error = status == HttpStatus.BAD_REQUEST ? "Validation Failed" : status.getReasonPhrase();
        this.messages = messages;
        this.path = path;
    }

    /**
     * Constructor đầy đủ với error code.
     *
     * @param status HTTP status
     * @param message Thông điệp lỗi
     * @param path Request path
     * @param errorCode Mã lỗi tùy chỉnh
     */
    public ApiError(HttpStatus status, String message, String path, String errorCode) {
        this.timestamp = getCurrentTimestamp();
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
        this.path = path;
        this.errorCode = errorCode;
    }

    /**
     * Tạo timestamp hiện tại theo format ISO-8601 UTC.
     *
     * @return Timestamp string
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

    /**
     * Factory method: tạo ApiError từ HttpStatus và message.
     *
     * @param status HTTP status
     * @param message Thông điệp lỗi
     * @param path Request path
     * @return ApiError instance
     */
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(status, message, path);
    }

    /**
     * Factory method: tạo ApiError từ HttpStatus và multiple messages.
     *
     * @param status HTTP status
     * @param messages Danh sách thông điệp lỗi
     * @param path Request path
     * @return ApiError instance
     */
    public static ApiError of(HttpStatus status, List<String> messages, String path) {
        return new ApiError(status, messages, path);
    }

    /**
     * Factory method: tạo ApiError với error code.
     *
     * @param status HTTP status
     * @param message Thông điệp lỗi
     * @param path Request path
     * @param errorCode Mã lỗi tùy chỉnh
     * @return ApiError instance
     */
    public static ApiError of(HttpStatus status, String message, String path, String errorCode) {
        return new ApiError(status, message, path, errorCode);
    }
}
