package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserPasswordRequest(
        @NotBlank(message = "Не заполнен старый пароль")
        String oldPassword,
        @NotBlank(message = "Не заполнен новый пароль")
        String newPassword
) {
}
