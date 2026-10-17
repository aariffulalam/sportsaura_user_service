package com.sportsaura.userservice.dto.response;

/**
 * Returned by register, login and refresh.
 *
 * @param accessToken short-lived JWT; send it as {@code Authorization: Bearer <token>}
 * @param refreshToken long-lived token; send it to {@code /auth/refresh} for a new pair. Each one
 *     works only once.
 * @param expiresInSeconds lifetime of the access token
 * @param refreshExpiresInSeconds lifetime of the refresh token
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    long refreshExpiresInSeconds,
    UserResponse user) {

  public static AuthResponse bearer(
      String accessToken,
      long expiresInSeconds,
      IssuedRefreshToken refreshToken,
      UserResponse user) {
    return new AuthResponse(
        accessToken,
        refreshToken.token(),
        "Bearer",
        expiresInSeconds,
        refreshToken.expiresInSeconds(),
        user);
  }
}
