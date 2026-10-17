package com.sportsaura.userservice.exception;

/** A refresh token is unknown, expired or already used. Mapped to HTTP 401. */
public class InvalidTokenException extends RuntimeException {

  public InvalidTokenException(String message) {
    super(message);
  }
}
