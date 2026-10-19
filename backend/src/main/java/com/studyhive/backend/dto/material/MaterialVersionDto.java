package com.studyhive.backend.dto.material;

import com.studyhive.backend.entity.MaterialVersion;
import java.time.Instant;

public record MaterialVersionDto(
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

    public static MaterialVersionDto from(MaterialVersion v) {
        return new MaterialVersionDto(
                v.getVersionId(),
                v.getMaterial().getMaterialId(),
                v.getVersionNumber(),
                v.getOriginalFileName(),
                v.getMimeType(),
                v.getFileSizeBytes(),
                v.getChecksumSha256(),
                v.getUploadedBy().getUserId(),
                v.getUploadedBy().getUsername(),
                v.getVersionNotes(),
                v.getCreatedAt());
    }
}
