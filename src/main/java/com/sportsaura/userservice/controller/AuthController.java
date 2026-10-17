package com.sportsaura.userservice.controller;

import com.sportsaura.userservice.dto.request.LoginRequest;
import com.sportsaura.userservice.dto.request.RefreshTokenRequest;
import com.sportsaura.userservice.dto.request.RegisterRequest;
import com.sportsaura.userservice.dto.response.AuthResponse;
import com.sportsaura.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Create an account and log in. No token needed.")
@SecurityRequirements
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new USER or TRAINER account")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  @Operation(summary = "Log in with email and password")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  @Operation(summary = "Get a new access token and refresh token using a refresh token")
  public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refresh(request);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Log out: the refresh token stops working")
  public void logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request);
  }
}
