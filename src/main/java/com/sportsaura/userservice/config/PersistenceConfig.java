package com.sportsaura.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Turns on automatic filling of {@code createdAt} and {@code updatedAt} on entities. */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {}
