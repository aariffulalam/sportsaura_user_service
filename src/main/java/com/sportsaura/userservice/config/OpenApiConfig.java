package com.sportsaura.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI setup, including the "Authorize" button for pasting a JWT. */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "SportsAura User Service API",
            version = "v1",
            description = "Registration, login and user profiles for SportsAura."),
    security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
@SecurityScheme(
    name = OpenApiConfig.BEARER_SCHEME,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {

  public static final String BEARER_SCHEME = "bearerAuth";
}
