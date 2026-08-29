package com.bitalep.exception;

import com.bitalep.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        echoRequestId(req);
        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        echoRequestId(req);
        List<ApiErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "errors:validation", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        echoRequestId(req);
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "errors:validation"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpServletRequest req) {
        echoRequestId(req);
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "errors:validation"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleSize(HttpServletRequest req) {
        echoRequestId(req);
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "errors:fileTooLarge"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleDenied(HttpServletRequest req) {
        echoRequestId(req);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of("FORBIDDEN", "errors:forbidden"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(HttpServletRequest req) {
        echoRequestId(req);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of("UNAUTHORIZED", "errors:unauthorized"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception ex, HttpServletRequest req) {
        String cid = echoRequestId(req);
        log.error("Unhandled error cid={}", cid, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "errors:internal"));
    }

    private static String echoRequestId(HttpServletRequest req) {
        String id = req.getHeader("X-Request-Id");
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }
}
