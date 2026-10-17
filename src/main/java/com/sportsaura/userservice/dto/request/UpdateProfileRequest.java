package com.sportsaura.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of {@code PUT /api/v1/users/me}. Sending a null phone clears it. */
public record UpdateProfileRequest(
    @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Phone must be 7 to 15 digits, optionally starting with +")
        String phone) {}
