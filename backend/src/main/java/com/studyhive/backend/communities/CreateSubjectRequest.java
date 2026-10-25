package com.studyhive.backend.communities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
        @NotBlank @Size(max = 30) String subjectCode, @NotBlank @Size(max = 150) String subjectName) {
}
