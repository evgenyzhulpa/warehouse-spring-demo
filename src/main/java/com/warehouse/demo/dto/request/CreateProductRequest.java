package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotBlank(message = "Не заполнено наименование номенклатуры")
        String name,
        @NotBlank(message = "Не заполнен артикул номенклатуры")
        String article,
        @NotNull(message = "Не заполнена базовая единица измерения")
        Long unitId
) {}
