package com.sportsaura.userservice.controller;

import com.sportsaura.userservice.dto.request.ChangePasswordRequest;
import com.sportsaura.userservice.dto.request.UpdateProfileRequest;
import com.sportsaura.userservice.dto.response.UserResponse;
import com.sportsaura.userservice.security.UserPrincipal;
import com.sportsaura.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profiles. Every endpoint needs a Bearer token.")
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  @Operation(summary = "Get my own profile")
  public UserResponse getMe(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
    return userService.getById(me.getId());
  }

  @PutMapping("/me")
  @Operation(summary = "Update my name and phone")
  public UserResponse updateMe(
      @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me,
      @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateProfile(me.getId(), request);
  }

  @PatchMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Change my password")
  public void changeMyPassword(
      @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me,
      @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(me.getId(), request);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "List all users, paged (ADMIN only)")
  public PagedModel<UserResponse> list(@ParameterObject Pageable pageable) {
    return new PagedModel<>(userService.list(pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get any user by id (ADMIN only)")
  public UserResponse getById(@PathVariable Long id) {
    return userService.getById(id);
  }
}
