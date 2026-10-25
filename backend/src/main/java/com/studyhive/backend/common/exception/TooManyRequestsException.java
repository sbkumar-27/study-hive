package com.studyhive.backend.common.exception;

/**
 * Thrown when a client has exceeded a rate limit (e.g. too many failed
 * community-password attempts in a rolling window). Maps to HTTP 429 in
 * {@link GlobalExceptionHandler}.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
