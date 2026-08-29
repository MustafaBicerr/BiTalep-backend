package com.bitalep.dto;

public record PaginationMeta(
        int page,
        int pageSize,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrev
) {
    public static PaginationMeta of(int page, int pageSize, long totalItems) {
        int totalPages = (int) Math.max(1, Math.ceil(totalItems / (double) Math.max(pageSize, 1)));
        return new PaginationMeta(
                page,
                pageSize,
                totalItems,
                totalPages,
                page < totalPages,
                page > 1
        );
    }
}
