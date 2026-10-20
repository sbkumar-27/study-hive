package com.studyhive.desktop.model;

import java.time.Instant;

public record MaterialVersion(
        Long versionId,
        Long materialId,
        int versionNumber,
        String originalFileName,
        String mimeType,
        long fileSizeBytes,
        String checksumSha256,
        Long uploadedByUserId,
        String uploadedByUsername,
        String versionNotes,
        Instant createdAt) {
}
