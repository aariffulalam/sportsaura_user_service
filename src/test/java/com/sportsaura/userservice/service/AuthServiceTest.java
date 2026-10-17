package com.sportsaura.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportsaura.userservice.dto.request.RefreshTokenRequest;
import com.sportsaura.userservice.dto.request.RegisterRequest;
import com.sportsaura.userservice.dto.response.AuthResponse;
import com.sportsaura.userservice.dto.response.IssuedRefreshToken;
import com.sportsaura.userservice.entity.Role;
import com.sportsaura.userservice.entity.User;
import com.sportsaura.userservice.exception.DuplicateResourceException;
import com.sportsaura.userservice.exception.InvalidRequestException;
import com.sportsaura.userservice.exception.InvalidTokenException;
import com.sportsaura.userservice.mapper.UserMapper;
import com.sportsaura.userservice.repository.UserRepository;
import com.sportsaura.userservice.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userRepository,
            passwordEncoder,
            authenticationManager,
            jwtService,
            refreshTokenService,
            new UserMapper());
  }

  @Test
  void registerHashesPasswordNormalizesEmailAndDefaultsToUserRole() {
    when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
    when(passwordEncoder.encode("secret-password")).thenReturn("hashed");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User saved = invocation.getArgument(0);
              saved.setId(1L);
              return saved;
            });
    stubTokens(1L);

    AuthResponse response =
        authService.register(
            new RegisterRequest(" Asha ", " Asha@Example.com ", "secret-password", null, null));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getEmail()).isEqualTo("asha@example.com");
    assertThat(saved.getName()).isEqualTo("Asha");
    assertThat(saved.getPasswordHash()).isEqualTo("hashed");
    assertThat(saved.getRole()).isEqualTo(Role.USER);
    assertThat(saved.isActive()).isTrue();

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.user().id()).isEqualTo(1L);
  }

  @Test
  void registerRejectsDuplicateEmail() {
    when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

    assertThatThrownBy(
            () ->
                authService.register(
                    new RegisterRequest(
                        "Asha", "asha@example.com", "secret-password", null, Role.TRAINER)))
        .isInstanceOf(DuplicateResourceException.class);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void registerRejectsAdminRole() {
    assertThatThrownBy(
            () ->
                authService.register(
                    new RegisterRequest(
                        "Asha", "asha@example.com", "secret-password", null, Role.ADMIN)))
        .isInstanceOf(InvalidRequestException.class);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void refreshIssuesNewTokensForActiveUser() {
    User user = User.builder().id(1L).email("asha@example.com").role(Role.USER).build();
    when(refreshTokenService.consume("old-refresh")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    stubTokens(1L);

    AuthResponse response = authService.refresh(new RefreshTokenRequest("old-refresh"));

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
  }

  @Test
  void refreshRejectsDeactivatedUser() {
    User user =
        User.builder().id(1L).email("asha@example.com").role(Role.USER).active(false).build();
    when(refreshTokenService.consume("old-refresh")).thenReturn(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("old-refresh")))
        .isInstanceOf(InvalidTokenException.class);
    verify(refreshTokenService, never()).issue(any());
  }

  @Test
  void logoutRevokesTheRefreshToken() {
    authService.logout(new RefreshTokenRequest("my-refresh"));

    verify(refreshTokenService).revoke("my-refresh");
  }

  private void stubTokens(Long userId) {
    when(jwtService.generateToken(any(User.class))).thenReturn("access-token");
    when(jwtService.getExpirationSeconds()).thenReturn(900L);
    when(refreshTokenService.issue(userId))
        .thenReturn(new IssuedRefreshToken("refresh-token", 2_592_000L));
  }
}
