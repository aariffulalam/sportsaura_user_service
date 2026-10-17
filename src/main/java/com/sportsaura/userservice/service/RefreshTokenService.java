package com.sportsaura.userservice.service;

import com.sportsaura.userservice.config.RefreshTokenProperties;
import com.sportsaura.userservice.dto.response.IssuedRefreshToken;
import com.sportsaura.userservice.entity.RefreshToken;
import com.sportsaura.userservice.exception.InvalidTokenException;
import com.sportsaura.userservice.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, checks and revokes refresh tokens.
 *
 * <p>Tokens rotate: each refresh token can be used once and is replaced by a new one. If a token
 * that was already used shows up again, it has probably been stolen, so every token of that user is
 * revoked and they must log in again.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final int TOKEN_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String INVALID_MESSAGE = "Refresh token is invalid or expired";

  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenProperties properties;

  @Transactional
  public IssuedRefreshToken issue(Long userId) {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Duration timeToLive = Duration.ofDays(properties.expirationDays());

    refreshTokenRepository.save(
        RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(token))
            .expiresAt(Instant.now().plus(timeToLive))
            .build());
    return new IssuedRefreshToken(token, timeToLive.toSeconds());
  }

  /**
   * Checks the token, marks it used and returns the id of the user it belongs to.
   *
   * <p>Does not roll back on {@link InvalidTokenException}, so a reuse-triggered revocation of all
   * the user's tokens is saved even though the request fails.
   */
  @Transactional(noRollbackFor = InvalidTokenException.class)
  public Long consume(String token) {
    Instant now = Instant.now();
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(hash(token))
            .orElseThrow(() -> new InvalidTokenException(INVALID_MESSAGE));

    if (refreshToken.isRevoked()) {
      refreshTokenRepository.revokeAllForUser(refreshToken.getUserId(), now);
      throw new InvalidTokenException(INVALID_MESSAGE);
    }
    if (refreshToken.isExpired(now)) {
      throw new InvalidTokenException(INVALID_MESSAGE);
    }
    refreshToken.setRevokedAt(now);
    return refreshToken.getUserId();
  }

  /** Revokes one token (logout). Unknown or already-revoked tokens are ignored. */
  @Transactional
  public void revoke(String token) {
    refreshTokenRepository
        .findByTokenHash(hash(token))
        .filter(refreshToken -> !refreshToken.isRevoked())
        .ifPresent(refreshToken -> refreshToken.setRevokedAt(Instant.now()));
  }

  /** Revokes every token of a user, logging them out on all devices. */
  @Transactional
  public void revokeAllForUser(Long userId) {
    refreshTokenRepository.revokeAllForUser(userId, Instant.now());
  }

  static String hash(String token) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
