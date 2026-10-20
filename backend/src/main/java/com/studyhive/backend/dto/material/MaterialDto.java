package com.studyhive.backend.dto.material;

import com.studyhive.backend.entity.Material;
import com.studyhive.backend.entity.MaterialType;
import com.studyhive.backend.entity.MaterialVisibility;
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
        Instant createdAt,
        Instant updatedAt) {

    public static MaterialDto of(
            Material m, long viewCount, boolean favorited, int latestVersionNumber) {
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
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
