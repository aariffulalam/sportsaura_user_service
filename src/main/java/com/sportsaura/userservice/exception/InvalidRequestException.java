package com.sportsaura.userservice.exception;

/** The request is well-formed but breaks a business rule. Mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

  public InvalidRequestException(String message) {
    super(message);
  }
}
