package com.sportsaura.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsaura.userservice.config.JwtProperties;
import com.sportsaura.userservice.entity.Role;
import com.sportsaura.userservice.entity.User;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "unit-test-secret-key-with-at-least-32-characters";

  private final User user =
      User.builder().id(42L).email("asha@example.com").name("Asha").role(Role.USER).build();

  @Test
  void tokenRoundTripReturnsUserId() {
    JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));

    String token = jwtService.generateToken(user);

    assertThat(jwtService.extractUserId(token)).contains(42L);
    assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600);
  }

  @Test
  void tokenSignedWithAnotherSecretIsRejected() {
    JwtService issuer = new JwtService(new JwtProperties(SECRET, 60));
    JwtService other =
        new JwtService(new JwtProperties("a-completely-different-secret-key-of-32-chars", 60));

    assertThat(other.extractUserId(issuer.generateToken(user))).isEmpty();
  }

  @Test
  void expiredTokenIsRejected() {
    JwtService jwtService = new JwtService(new JwtProperties(SECRET, -1));

    assertThat(jwtService.extractUserId(jwtService.generateToken(user))).isEmpty();
  }

  @Test
  void garbageTokenIsRejected() {
    JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));

    assertThat(jwtService.extractUserId("not-a-token")).isEmpty();
  }
}
