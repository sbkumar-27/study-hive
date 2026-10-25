package com.studyhive.backend.materials;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull Long materialId, @NotBlank @Size(max = 255) String reason) {
}
