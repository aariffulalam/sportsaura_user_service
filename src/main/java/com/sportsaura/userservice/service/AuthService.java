package com.sportsaura.userservice.service;

import com.sportsaura.userservice.dto.request.LoginRequest;
import com.sportsaura.userservice.dto.request.RefreshTokenRequest;
import com.sportsaura.userservice.dto.request.RegisterRequest;
import com.sportsaura.userservice.dto.response.AuthResponse;
import com.sportsaura.userservice.entity.Role;
import com.sportsaura.userservice.entity.User;
import com.sportsaura.userservice.exception.DuplicateResourceException;
import com.sportsaura.userservice.exception.InvalidRequestException;
import com.sportsaura.userservice.exception.InvalidTokenException;
import com.sportsaura.userservice.exception.ResourceNotFoundException;
import com.sportsaura.userservice.mapper.UserMapper;
import com.sportsaura.userservice.repository.UserRepository;
import com.sportsaura.userservice.security.JwtService;
import com.sportsaura.userservice.security.UserPrincipal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for creating accounts, logging in and out, and refreshing tokens. */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final UserMapper userMapper;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    Role role = request.role() == null ? Role.USER : request.role();
    if (role == Role.ADMIN) {
      throw new InvalidRequestException("Admin accounts cannot be created through registration");
    }
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new DuplicateResourceException("An account with this email already exists");
    }

    User user =
        userRepository.save(
            User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(role)
                .build());
    return toAuthResponse(user);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                normalizeEmail(request.email()), request.password()));
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    User user =
        userRepository
            .findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return toAuthResponse(user);
  }

  /** Swaps a refresh token for a new access token and a new refresh token. */
  @Transactional(noRollbackFor = InvalidTokenException.class)
  public AuthResponse refresh(RefreshTokenRequest request) {
    Long userId = refreshTokenService.consume(request.refreshToken());
    User user =
        userRepository
            .findById(userId)
            .filter(User::isActive)
            .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or expired"));
    return toAuthResponse(user);
  }

  /** Ends the session of this refresh token. The current access token expires on its own. */
  @Transactional
  public void logout(RefreshTokenRequest request) {
    refreshTokenService.revoke(request.refreshToken());
  }

  private AuthResponse toAuthResponse(User user) {
    return AuthResponse.bearer(
        jwtService.generateToken(user),
        jwtService.getExpirationSeconds(),
        refreshTokenService.issue(user.getId()),
        userMapper.toResponse(user));
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
