package com.warehouse.demo.dto.response;

public record ValidationFieldError(
        String field,
        String message
) {
}
