package com.sportsaura.userservice.exception;

/** A record with the same unique value already exists. Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

  public DuplicateResourceException(String message) {
    super(message);
  }
}
