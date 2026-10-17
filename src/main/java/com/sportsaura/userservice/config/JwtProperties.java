package com.sportsaura.userservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under {@code app.jwt} in application.yml. The app refuses to start if they are invalid.
 *
 * @param secret key used to sign tokens; at least 32 characters
 * @param expirationMinutes how long a token stays valid
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    @NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
    @Min(1) long expirationMinutes) {}
