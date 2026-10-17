package com.sportsaura.userservice.security;

import com.sportsaura.userservice.config.JwtProperties;
import com.sportsaura.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Creates and checks JWT access tokens. The token's subject is the user id. */
@Service
public class JwtService {

  private final SecretKey signingKey;
  private final Duration timeToLive;

  public JwtService(JwtProperties properties) {
    this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    this.timeToLive = Duration.ofMinutes(properties.expirationMinutes());
  }

  public String generateToken(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim("email", user.getEmail())
        .claim("role", user.getRole().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(timeToLive)))
        .signWith(signingKey)
        .compact();
  }

  /** Returns the user id inside the token, or empty if it is expired, tampered with or garbage. */
  public Optional<Long> extractUserId(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
      return Optional.of(Long.valueOf(claims.getSubject()));
    } catch (JwtException | IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  public long getExpirationSeconds() {
    return timeToLive.toSeconds();
  }
}
