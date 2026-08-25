package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUnitRequest(
        @NotBlank(message = "Не заполнено имя единицы измерения")
        String name,
        @NotBlank(message = "Не заполнен код единицы измерения")
        String code,
        String description
) {}
