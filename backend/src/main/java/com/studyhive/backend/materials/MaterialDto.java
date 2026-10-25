package com.studyhive.backend.materials;

import com.studyhive.backend.materials.Material;
import com.studyhive.backend.materials.MaterialType;
import com.studyhive.backend.materials.MaterialVisibility;
import java.time.Instant;

public record MaterialDto(
        Long materialId,
        Long communityId,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long uploadedByUserId,
        String uploadedByUsername,
        String title,
        String description,
        MaterialType materialType,
        String bookName,
        String authorName,
        MaterialVisibility visibility,
        long viewCount,
        boolean favorited,
        int latestVersionNumber,
        String latestVersionFileName,
        String latestVersionMimeType,
        Long latestVersionSizeBytes,
        Instant createdAt,
        Instant updatedAt) {

    public static MaterialDto of(
            Material m,
            long viewCount,
            boolean favorited,
            int latestVersionNumber,
            String latestVersionFileName,
            String latestVersionMimeType,
            Long latestVersionSizeBytes) {
        return new MaterialDto(
                m.getMaterialId(),
                m.getCommunity() == null ? null : m.getCommunity().getCommunityId(),
                m.getSubject() == null ? null : m.getSubject().getSubjectId(),
                m.getSubject() == null ? null : m.getSubject().getSubjectCode(),
                m.getSubject() == null ? null : m.getSubject().getSubjectName(),
                m.getUploadedBy().getUserId(),
                m.getUploadedBy().getUsername(),
                m.getTitle(),
                m.getDescription(),
                m.getMaterialType(),
                m.getBookName(),
                m.getAuthorName(),
                m.getVisibility(),
                viewCount,
                favorited,
                latestVersionNumber,
                latestVersionFileName,
                latestVersionMimeType,
                latestVersionSizeBytes,
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
