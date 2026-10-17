package com.sportsaura.userservice.security;

import com.sportsaura.userservice.entity.User;
import com.sportsaura.userservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs once per request. If the request carries a valid {@code Authorization: Bearer <token>}
 * header for an active user, marks the request as logged in. Otherwise does nothing and lets Spring
 * Security reject the request later if the URL needs a login.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null
        && header.startsWith(BEARER_PREFIX)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      String token = header.substring(BEARER_PREFIX.length());
      jwtService
          .extractUserId(token)
          .flatMap(userRepository::findById)
          .filter(User::isActive)
          .ifPresent(user -> authenticate(user, request));
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(User user, HttpServletRequest request) {
    UserPrincipal principal = UserPrincipal.from(user);
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
