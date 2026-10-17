package com.sportsaura.userservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the migrations work on real MySQL. The app starting at all already proves that Hibernate
 * found the tables it expects ({@code ddl-auto: validate}).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class FlywayMigrationIntegrationTest {

  @Autowired private Flyway flyway;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void everyMigrationIsAppliedSuccessfully() {
    MigrationInfo[] applied = flyway.info().applied();

    assertThat(applied).isNotEmpty();
    assertThat(applied).allMatch(info -> info.getState() == MigrationState.SUCCESS);
    assertThat(flyway.info().pending()).isEmpty();
    assertThat(Arrays.stream(applied).map(info -> info.getVersion().getVersion()))
        .containsExactly("1", "2");
  }

  @Test
  void expectedTablesExist() {
    List<String> tables =
        jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
            String.class);

    assertThat(tables).contains("users", "refresh_tokens", "flyway_schema_history");
  }

  @Test
  void emailMustBeUnique() {
    Integer uniqueIndexes =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE()"
                + " AND table_name = 'users' AND column_name = 'email' AND non_unique = 0",
            Integer.class);

    assertThat(uniqueIndexes).isEqualTo(1);
  }

  @Test
  void deletingAUserDeletesTheirRefreshTokens() {
    jdbcTemplate.update(
        "INSERT INTO users (email, name, password_hash, role, active, created_at, updated_at)"
            + " VALUES ('cascade@example.com', 'C', 'x', 'USER', 1, NOW(6), NOW(6))");
    Long userId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'cascade@example.com'", Long.class);
    jdbcTemplate.update(
        "INSERT INTO refresh_tokens (user_id, token_hash, expires_at, created_at, updated_at)"
            + " VALUES (?, REPEAT('a', 64), NOW(6), NOW(6), NOW(6))",
        userId);

    jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", Integer.class, userId))
        .isZero();
  }
}
