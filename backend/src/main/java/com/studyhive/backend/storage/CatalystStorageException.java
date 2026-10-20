package com.studyhive.backend.storage;

/** Wraps any Catalyst SDK/config failure; never caught to fall back to local storage. */
public class CatalystStorageException extends RuntimeException {

    public CatalystStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public CatalystStorageException(String message) {
        super(message);
    }
}
