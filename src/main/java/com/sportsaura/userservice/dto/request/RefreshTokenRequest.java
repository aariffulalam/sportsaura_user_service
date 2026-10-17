package com.sportsaura.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/v1/auth/refresh} and {@code POST /api/v1/auth/logout}. */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required") String refreshToken) {}
