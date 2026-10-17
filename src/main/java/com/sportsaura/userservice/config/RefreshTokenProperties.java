package com.sportsaura.userservice.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under {@code app.refresh-token} in application.yml.
 *
 * @param expirationDays how long a refresh token stays valid; the user must log in again after
 */
@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(@Min(1) long expirationDays) {}
