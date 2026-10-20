package com.studyhive.backend.storage;

/**
 * Result of persisting an uploaded file: everything a material_versions row needs.
 */
public record StoredFile(
        String storageKey,
        long fileSizeBytes,
        String checksumSha256) {
}
