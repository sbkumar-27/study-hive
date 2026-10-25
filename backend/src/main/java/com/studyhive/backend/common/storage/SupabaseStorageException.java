package com.studyhive.backend.common.storage;

/** Wraps any Supabase Storage REST API/config failure; never caught to fall back to local storage. */
public class SupabaseStorageException extends RuntimeException {

    public SupabaseStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public SupabaseStorageException(String message) {
        super(message);
    }
}
