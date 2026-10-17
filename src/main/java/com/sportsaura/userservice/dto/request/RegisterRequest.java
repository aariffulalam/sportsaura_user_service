package com.sportsaura.userservice.dto.request;

import com.sportsaura.userservice.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/auth/register}.
 *
 * @param role optional; USER (default) or TRAINER. ADMIN cannot be self-registered.
 */
public record RegisterRequest(
    @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,
    @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,
    @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Phone must be 7 to 15 digits, optionally starting with +")
        String phone,
    Role role) {}
