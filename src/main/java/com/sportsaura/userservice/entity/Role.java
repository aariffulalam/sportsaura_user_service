package com.sportsaura.userservice.entity;

/** What kind of account a user has. Stored as text in the {@code users.role} column. */
public enum Role {
  USER,
  TRAINER,
  ADMIN
}
