package com.sportsaura.userservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a real MySQL in Docker for integration tests and points the app at it.
 *
 * <p>Same MySQL version as docker-compose.yml. Flyway runs every migration on it at startup and
 * Hibernate validates the tables, exactly like in production. The container is shared by all test
 * classes that import this configuration.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  MySQLContainer<?> mysqlContainer() {
    return new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));
  }
}
