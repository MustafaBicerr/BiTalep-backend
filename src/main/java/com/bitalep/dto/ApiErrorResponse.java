package com.bitalep.dto;

import java.util.List;

public record ApiErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message, List<FieldError> details) {}

    public record FieldError(String field, String message) {}

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(new ErrorBody(code, message, null));
    }

    public static ApiErrorResponse of(String code, String message, List<FieldError> details) {
        return new ApiErrorResponse(new ErrorBody(code, message, details));
    }
}
