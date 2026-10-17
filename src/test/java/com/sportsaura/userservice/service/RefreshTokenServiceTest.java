package com.sportsaura.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportsaura.userservice.config.RefreshTokenProperties;
import com.sportsaura.userservice.dto.response.IssuedRefreshToken;
import com.sportsaura.userservice.entity.RefreshToken;
import com.sportsaura.userservice.exception.InvalidTokenException;
import com.sportsaura.userservice.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    refreshTokenService =
        new RefreshTokenService(refreshTokenRepository, new RefreshTokenProperties(30));
  }

  @Test
  void issueStoresOnlyTheHashOfTheToken() {
    IssuedRefreshToken issued = refreshTokenService.issue(7L);

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    RefreshToken saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(7L);
    assertThat(saved.getTokenHash()).isEqualTo(RefreshTokenService.hash(issued.token()));
    assertThat(saved.getTokenHash()).isNotEqualTo(issued.token());
    assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(29)));
    assertThat(issued.expiresInSeconds()).isEqualTo(Duration.ofDays(30).toSeconds());
  }

  @Test
  void issueCreatesADifferentTokenEachTime() {
    assertThat(refreshTokenService.issue(7L).token())
        .isNotEqualTo(refreshTokenService.issue(7L).token());
  }

  @Test
  void consumeValidTokenMarksItUsedAndReturnsUserId() {
    RefreshToken stored = storedToken("good", Instant.now().plus(Duration.ofDays(1)), null);

    assertThat(refreshTokenService.consume("good")).isEqualTo(7L);
    assertThat(stored.isRevoked()).isTrue();
  }

  @Test
  void consumeUnknownTokenIsRejected() {
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> refreshTokenService.consume("unknown"))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void consumeExpiredTokenIsRejected() {
    storedToken("old", Instant.now().minus(Duration.ofMinutes(1)), null);

    assertThatThrownBy(() -> refreshTokenService.consume("old"))
        .isInstanceOf(InvalidTokenException.class);
    verify(refreshTokenRepository, never()).revokeAllForUser(anyLong(), any());
  }

  @Test
  void reusingAUsedTokenRevokesEveryTokenOfTheUser() {
    storedToken("stolen", Instant.now().plus(Duration.ofDays(1)), Instant.now().minusSeconds(60));

    assertThatThrownBy(() -> refreshTokenService.consume("stolen"))
        .isInstanceOf(InvalidTokenException.class);
    verify(refreshTokenRepository).revokeAllForUser(eq(7L), any(Instant.class));
  }

  @Test
  void revokeMarksTokenUsed() {
    RefreshToken stored = storedToken("mine", Instant.now().plus(Duration.ofDays(1)), null);

    refreshTokenService.revoke("mine");

    assertThat(stored.isRevoked()).isTrue();
  }

  private RefreshToken storedToken(String token, Instant expiresAt, Instant revokedAt) {
    RefreshToken stored =
        RefreshToken.builder()
            .id(1L)
            .userId(7L)
            .tokenHash(RefreshTokenService.hash(token))
            .expiresAt(expiresAt)
            .revokedAt(revokedAt)
            .build();
    when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(token)))
        .thenReturn(Optional.of(stored));
    return stored;
  }
}
