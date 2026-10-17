package com.sportsaura.userservice.service;

import com.sportsaura.userservice.dto.request.ChangePasswordRequest;
import com.sportsaura.userservice.dto.request.UpdateProfileRequest;
import com.sportsaura.userservice.dto.response.UserResponse;
import com.sportsaura.userservice.entity.User;
import com.sportsaura.userservice.exception.InvalidRequestException;
import com.sportsaura.userservice.exception.ResourceNotFoundException;
import com.sportsaura.userservice.mapper.UserMapper;
import com.sportsaura.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for reading and changing user profiles. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;

  public UserResponse getById(Long id) {
    return userMapper.toResponse(findUser(id));
  }

  public Page<UserResponse> list(Pageable pageable) {
    return userRepository.findAll(pageable).map(userMapper::toResponse);
  }

  @Transactional
  public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
    User user = findUser(id);
    user.setName(request.name().trim());
    user.setPhone(request.phone());
    return userMapper.toResponse(userRepository.saveAndFlush(user));
  }

  /** Changes the password and logs the user out on every device (all refresh tokens revoked). */
  @Transactional
  public void changePassword(Long id, ChangePasswordRequest request) {
    User user = findUser(id);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new InvalidRequestException("Current password is incorrect");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    refreshTokenService.revokeAllForUser(id);
  }

  private User findUser(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
  }
}
