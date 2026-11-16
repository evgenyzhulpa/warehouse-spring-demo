package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateIncomeDocumentRequest(
        @NotNull(message = "Не заполнен склад")
        Long warehouseId,
        LocalDateTime documentDate
) {}
