package com.studyhive.desktop.model;

import java.time.Instant;

public record Material(
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
}
