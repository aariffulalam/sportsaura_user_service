package com.sportsaura.userservice.mapper;

import com.sportsaura.userservice.dto.response.UserResponse;
import com.sportsaura.userservice.entity.User;
import org.springframework.stereotype.Component;

/** Converts entities to API responses, so entities never leave the service layer. */
@Component
public class UserMapper {

  public UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getPhone(),
        user.getRole(),
        user.isActive(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
