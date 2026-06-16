package com.warehouse.demo.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String path,
        List<ValidationFieldError> errors
) {
}
