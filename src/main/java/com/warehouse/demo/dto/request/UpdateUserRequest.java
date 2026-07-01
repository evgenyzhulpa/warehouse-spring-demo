package com.warehouse.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateUserRequest(
        String username,
        Boolean enabled,
        String lastName,
        String firstName,
        String middleName,
        List<Long> rolesIdentifiers
) {
}
