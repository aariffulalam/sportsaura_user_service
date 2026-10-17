package com.sportsaura.userservice.security;

import com.sportsaura.userservice.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets Spring Security look a user up by email when checking a login. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) {
    return userRepository
        .findByEmail(email.trim().toLowerCase(Locale.ROOT))
        .map(UserPrincipal::from)
        .orElseThrow(() -> new UsernameNotFoundException("No user with this email"));
  }
}
