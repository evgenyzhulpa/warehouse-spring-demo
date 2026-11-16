package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateWarehouseRequest(
        @NotBlank(message = "Не заполнено наименование склада")
        String name,
        String address
) {}
