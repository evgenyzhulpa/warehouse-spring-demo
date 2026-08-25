package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Не указан токен обновления")
        String refreshToken) {
}
