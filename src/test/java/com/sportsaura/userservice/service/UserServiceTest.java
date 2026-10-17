package com.sportsaura.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportsaura.userservice.dto.request.ChangePasswordRequest;
import com.sportsaura.userservice.dto.request.UpdateProfileRequest;
import com.sportsaura.userservice.dto.response.UserResponse;
import com.sportsaura.userservice.entity.Role;
import com.sportsaura.userservice.entity.User;
import com.sportsaura.userservice.exception.InvalidRequestException;
import com.sportsaura.userservice.exception.ResourceNotFoundException;
import com.sportsaura.userservice.mapper.UserMapper;
import com.sportsaura.userservice.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private RefreshTokenService refreshTokenService;

  private UserService userService;
  private User user;

  @BeforeEach
  void setUp() {
    userService =
        new UserService(userRepository, new UserMapper(), passwordEncoder, refreshTokenService);
    user =
        User.builder()
            .id(1L)
            .name("Asha")
            .email("asha@example.com")
            .passwordHash("old-hash")
            .role(Role.USER)
            .build();
  }

  @Test
  void getByIdThrowsWhenUserDoesNotExist() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void updateProfileChangesNameAndPhone() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.saveAndFlush(user)).thenReturn(user);

    UserResponse response =
        userService.updateProfile(1L, new UpdateProfileRequest(" Asha Rao ", "+919876543210"));

    assertThat(response.name()).isEqualTo("Asha Rao");
    assertThat(response.phone()).isEqualTo("+919876543210");
  }

  @Test
  void changePasswordStoresNewHashAndLogsOutEverywhere() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
    when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

    userService.changePassword(1L, new ChangePasswordRequest("old-password", "new-password"));

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    verify(userRepository).save(user);
    verify(refreshTokenService).revokeAllForUser(1L);
  }

  @Test
  void changePasswordRejectsWrongCurrentPassword() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

    assertThatThrownBy(
            () -> userService.changePassword(1L, new ChangePasswordRequest("wrong", "new-pass")))
        .isInstanceOf(InvalidRequestException.class);
    verify(userRepository, never()).save(any(User.class));
  }
}
