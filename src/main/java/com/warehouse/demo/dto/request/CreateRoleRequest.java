package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateRoleRequest(
        @NotBlank(message = "Не заполнено имя роли")
        String name,
        @NotEmpty(message = "Не указаны права роли")
        List<Long> permissionsIdentifiers
) {
}
