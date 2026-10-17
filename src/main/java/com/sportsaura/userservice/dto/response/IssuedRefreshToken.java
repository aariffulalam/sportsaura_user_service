package com.sportsaura.userservice.dto.response;

/**
 * A newly created refresh token, as handed to the client. The raw token exists only here; the
 * database keeps a hash.
 */
public record IssuedRefreshToken(String token, long expiresInSeconds) {}
