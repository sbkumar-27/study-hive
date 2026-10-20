package com.studyhive.backend.exception;

/**
 * Thrown by service-layer authorization checks (e.g. "not a member of this
 * community", "not the uploader or a moderator"). Distinct from Spring
 * Security's own AccessDeniedException so GlobalExceptionHandler can report
 * both with one consistent JSON body.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
