package com.bitalep.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "errors:unauthorized");
    }

    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "errors:forbidden");
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "errors:notFound");
    }

    public static ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", "errors:conflict");
    }

    public static ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public static ApiException subscriptionInactive() {
        return new ApiException(HttpStatus.PAYMENT_REQUIRED, "SUBSCRIPTION_INACTIVE", "errors:subscriptionInactive");
    }
}
