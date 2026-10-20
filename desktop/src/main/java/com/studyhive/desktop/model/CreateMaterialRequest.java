package com.studyhive.desktop.model;

public record CreateMaterialRequest(
        String title,
        String description,
        MaterialType materialType,
        Long communityId,
        Long subjectId,
        String bookName,
        String authorName,
        Integer examYear,
        String examType,
        Integer semester) {
}
