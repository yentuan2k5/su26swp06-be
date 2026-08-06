package com.swp391.scientific_journal_tracker.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // Lỗi validate: @NotBlank, @Email, @Size...
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationException(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                Map<String, String> errors = new HashMap<>();

                ex.getBindingResult().getFieldErrors().forEach(error -> {
                        errors.put(error.getField(), error.getDefaultMessage());
                });

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("error", "Bad Request");
                response.put("message", "Dữ liệu không hợp lệ");
                response.put("path", request.getRequestURI());
                response.put("errors", errors);

                return ResponseEntity.badRequest().body(response);
        }

        // Lỗi JSON sai format
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidJson(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "JSON gửi lên không hợp lệ",
                                request);
        }

        // Lỗi validate ở path variable, request param... khi controller dùng @Validated
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<Map<String, Object>> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {
                Map<String, String> errors = new HashMap<>();
                ex.getConstraintViolations().forEach(violation ->
                        errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("error", "Bad Request");
                response.put("message", "Dữ liệu không hợp lệ");
                response.put("path", request.getRequestURI());
                response.put("errors", errors);

                return ResponseEntity.badRequest().body(response);
        }

        // Lỗi trùng dữ liệu: username/email đã tồn tại
        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<Map<String, Object>> handleDuplicateResource(
                        DuplicateResourceException ex,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.CONFLICT,
                                ex.getMessage(),
                                request);
        }

        // Lỗi không tìm thấy dữ liệu
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleResourceNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.NOT_FOUND,
                                ex.getMessage(),
                                request);
        }

        // Lỗi request sai: password sai, role sai, token sai...
        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<Map<String, Object>> handleBadRequest(
                        BadRequestException ex,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getMessage(),
                                request);
        }

        // Lỗi chưa đăng nhập / token sai
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<Map<String, Object>> handleAuthentication(
                        AuthenticationException ex,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                "Bạn chưa đăng nhập hoặc token không hợp lệ",
                                request);
        }

        // Lỗi không có quyền
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handleAccessDenied(
                        AccessDeniedException ex,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.FORBIDDEN,
                                "Bạn không có quyền truy cập chức năng này",
                                request);
        }

        // Lỗi RuntimeException bình thường
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<Map<String, Object>> handleRuntimeException(
                        RuntimeException ex,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getMessage(),
                                request);
        }

        // Lỗi còn lại
        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGeneralException(
                        Exception ex,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Lỗi hệ thống, vui lòng thử lại sau",
                                request);
        }

        private ResponseEntity<Map<String, Object>> buildResponse(
                        HttpStatus status,
                        String message,
                        HttpServletRequest request) {
                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", status.value());
                response.put("error", status.getReasonPhrase());
                response.put("message", message);
                response.put("path", request.getRequestURI());

                return ResponseEntity.status(status).body(response);
        }

        @ExceptionHandler(EmailSendingException.class)
        public ResponseEntity<Map<String, Object>> handleEmailSendingException(
                        EmailSendingException ex,
                        HttpServletRequest request) {

                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
                response.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
                response.put("message", ex.getMessage());
                response.put("path", request.getRequestURI());

                return ResponseEntity
                                .status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(response);
        }
}
