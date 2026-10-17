package com.sportsaura.userservice.security;

import com.sportsaura.userservice.entity.Role;
import com.sportsaura.userservice.entity.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The logged-in user as Spring Security sees it. Controllers receive this, not the entity. */
@Getter
public class UserPrincipal implements UserDetails {

  private final Long id;
  private final String email;
  private final String passwordHash;
  private final Role role;
  private final boolean active;

  private UserPrincipal(Long id, String email, String passwordHash, Role role, boolean active) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.active = active;
  }

  public static UserPrincipal from(User user) {
    return new UserPrincipal(
        user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(), user.isActive());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
