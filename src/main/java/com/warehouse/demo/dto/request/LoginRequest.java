package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Не указано имя пользователя")
        String username,
        @NotBlank(message = "Не указан пароль")
        String password
) {
}
