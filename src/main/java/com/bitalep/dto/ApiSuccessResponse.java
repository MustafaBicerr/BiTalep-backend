package com.bitalep.dto;

import java.util.List;

public record ApiSuccessResponse<T>(T data, PaginationMeta meta) {

    public static <T> ApiSuccessResponse<T> of(T data) {
        return new ApiSuccessResponse<>(data, null);
    }

    public static <T> ApiSuccessResponse<List<T>> page(List<T> data, PaginationMeta meta) {
        return new ApiSuccessResponse<>(data, meta);
    }
}
