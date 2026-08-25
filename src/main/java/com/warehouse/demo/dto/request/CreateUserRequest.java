package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "Не заполнен логин пользователя")
        String username,
        @NotBlank(message = "Не заполнен пароль пользователя")
        String password,
        String lastName,
        String firstName,
        String middleName,
        @NotEmpty(message = "Не указаны роли пользователя")
        List<Long> rolesIdentifiers
) {
}
