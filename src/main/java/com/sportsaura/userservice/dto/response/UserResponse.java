package com.sportsaura.userservice.dto.response;

import com.sportsaura.userservice.entity.Role;
import java.time.Instant;

/** What the API returns for a user. Deliberately has no password field. */
public record UserResponse(
    Long id,
    String name,
    String email,
    String phone,
    Role role,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
