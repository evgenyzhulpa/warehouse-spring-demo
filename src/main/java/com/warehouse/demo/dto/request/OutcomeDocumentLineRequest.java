package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OutcomeDocumentLineRequest(
        @NotNull(message = "Не заполнена номенклатура")
        Long productId,
        @NotNull(message = "Не заполнено количество")
        @DecimalMin(value = "0", inclusive = false, message = "Количество должно быть больше нуля")
        @Digits(integer = 12, fraction = 3, message = "Количество: не более 12 знаков до запятой и 3 после")
        BigDecimal quantity
) {}
