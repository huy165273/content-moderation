package com.example.moderation.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global Exception Handler xử lý tất cả exception trong ứng dụng.
 * Đảm bảo mọi lỗi đều trả về JSON format chuẩn và thống nhất.
 *
 * Sử dụng @RestControllerAdvice để áp dụng cho tất cả @RestController.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi validation khi dùng @Valid trong controller.
     * Ví dụ: @NotBlank, @NotNull, @Size, @Pattern không thỏa mãn.
     *
     * @param ex MethodArgumentNotValidException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return String.format("%s: %s", fieldName, errorMessage);
                })
                .collect(Collectors.toList());

        String path = getRequestPath(request);

        log.warn("Validation failed for request {}: {}", path, errors);

        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST, errors, path);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý lỗi constraint violation (Jakarta Bean Validation).
     * Thường xảy ra khi validate method parameters hoặc path variables.
     *
     * @param ex ConstraintViolationException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request) {

        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        String path = getRequestPath(request);

        log.warn("Constraint violation for request {}: {}", path, errors);

        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST, errors, path);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý lỗi khi JSON request body không đọc được.
     * Ví dụ: JSON sai format, thiếu dấu ngoặc, sai kiểu dữ liệu.
     *
     * @param ex HttpMessageNotReadableException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        String path = getRequestPath(request);
        String message = "JSON request không hợp lệ hoặc sai định dạng";

        log.warn("Malformed JSON request at {}: {}", path, ex.getMessage());

        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST, message, path);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý lỗi khi thiếu required request parameter.
     * Ví dụ: ?concurrency=10 bị thiếu nhưng parameter required.
     *
     * @param ex MissingServletRequestParameterException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            WebRequest request) {

        String path = getRequestPath(request);
        String message = String.format("Thiếu parameter bắt buộc: '%s' (kiểu %s)",
                ex.getParameterName(), ex.getParameterType());

        log.warn("Missing parameter at {}: {}", path, ex.getParameterName());

        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST, message, path);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý lỗi khi HTTP method không được support.
     * Ví dụ: endpoint chỉ hỗ trợ POST nhưng client gọi GET.
     *
     * @param ex HttpRequestMethodNotSupportedException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {

        String path = getRequestPath(request);
        String message = String.format("HTTP method '%s' không được hỗ trợ cho endpoint này. " +
                "Các method được hỗ trợ: %s", ex.getMethod(), ex.getSupportedHttpMethods());

        log.warn("Unsupported HTTP method {} at {}", ex.getMethod(), path);

        ApiError apiError = ApiError.of(HttpStatus.METHOD_NOT_ALLOWED, message, path);

        return new ResponseEntity<>(apiError, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Xử lý lỗi khi parameter type không khớp.
     * Ví dụ: ?concurrency=abc nhưng expect Integer.
     *
     * @param ex MethodArgumentTypeMismatchException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        String path = getRequestPath(request);
        String message = String.format("Parameter '%s' có giá trị '%s' không hợp lệ. " +
                "Cần kiểu dữ liệu: %s",
                ex.getName(), ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        log.warn("Type mismatch for parameter {} at {}: {} (expected {})",
                ex.getName(), path, ex.getValue(), ex.getRequiredType());

        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST, message, path);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý custom EntityNotFoundException.
     * Trả về 404 NOT FOUND khi không tìm thấy entity.
     *
     * @param ex EntityNotFoundException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFoundException(
            EntityNotFoundException ex,
            WebRequest request) {

        String path = getRequestPath(request);

        log.warn("Entity not found at {}: {}", path, ex.getMessage());

        ApiError apiError = ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage(), path);

        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    /**
     * Xử lý custom BusinessException.
     * Trả về 422 UNPROCESSABLE ENTITY cho business logic errors.
     *
     * @param ex BusinessException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException ex,
            WebRequest request) {

        String path = getRequestPath(request);

        log.warn("Business logic error at {}: {}", path, ex.getMessage());

        ApiError apiError;
        if (ex.getErrorCode() != null) {
            apiError = ApiError.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), path, ex.getErrorCode());
        } else {
            apiError = ApiError.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), path);
        }

        return new ResponseEntity<>(apiError, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Xử lý custom ExternalApiException.
     * Trả về 502 BAD GATEWAY khi external API (Alibaba Cloud) lỗi.
     *
     * @param ex ExternalApiException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiError> handleExternalApiException(
            ExternalApiException ex,
            WebRequest request) {

        String path = getRequestPath(request);

        log.error("External API error at {}: {}", path, ex.getMessage(), ex);

        ApiError apiError = ApiError.of(HttpStatus.BAD_GATEWAY, ex.getMessage(), path);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_GATEWAY);
    }

    /**
     * Xử lý IllegalArgumentException.
     * Thường dùng cho validation logic đơn giản.
     *
     * @param ex IllegalArgumentException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        String path = getRequestPath(request);

        log.warn("Illegal argument at {}: {}", path, ex.getMessage());

        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST, ex.getMessage(), path);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý RuntimeException (catch-all cho runtime errors).
     * Không log stack trace ra ngoài, chỉ trả về message generic.
     *
     * @param ex RuntimeException
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {

        String path = getRequestPath(request);

        log.error("Runtime error at {}: {}", path, ex.getMessage(), ex);

        // Không trả về chi tiết exception ra ngoài để bảo mật
        ApiError apiError = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã xảy ra lỗi trong quá trình xử lý. Vui lòng thử lại sau.",
                path);

        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Xử lý tất cả Exception không được catch bởi handlers khác.
     * Đây là catch-all cuối cùng đảm bảo không có unhandled exception.
     *
     * @param ex Exception
     * @param request WebRequest
     * @return ResponseEntity với ApiError
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobalException(
            Exception ex,
            WebRequest request) {

        String path = getRequestPath(request);

        // Log chi tiết để debug
        log.error("Unhandled exception at {}: {}", path, ex.getMessage(), ex);

        // Trả về message generic, không leak stack trace
        ApiError apiError = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error. Vui lòng liên hệ administrator.",
                path);

        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Utility method để lấy request path từ WebRequest.
     *
     * @param request WebRequest
     * @return Request path string
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
            return httpRequest.getRequestURI();
        }
        return request.getDescription(false).replace("uri=", "");
    }
}
