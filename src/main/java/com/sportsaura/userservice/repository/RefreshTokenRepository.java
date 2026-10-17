package com.sportsaura.userservice.repository;

import com.sportsaura.userservice.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** Revokes every still-active refresh token of a user. Returns how many were revoked. */
  @Modifying
  @Query(
      "update RefreshToken t set t.revokedAt = :now, t.updatedAt = :now"
          + " where t.userId = :userId and t.revokedAt is null")
  int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
