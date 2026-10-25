package com.studyhive.backend.materials;

import com.studyhive.backend.materials.MaterialType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Bound from multipart form fields (not JSON) alongside the uploaded file.
 * {@code communityId} is omitted/null for a Personal Vault upload.
 */
public record CreateMaterialRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        MaterialType materialType,
        Long communityId,
        Long subjectId,
        @Size(max = 200) String bookName,
        @Size(max = 150) String authorName,
        // Question-paper-only fields, ignored unless materialType == QUESTION_PAPER.
        @Min(1900) @Max(2100) Integer examYear,
        @Size(max = 100) String examType,
        @Min(1) @Max(12) Integer semester) {
}
