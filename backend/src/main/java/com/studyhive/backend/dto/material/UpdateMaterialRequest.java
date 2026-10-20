package com.studyhive.backend.dto.material;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMaterialRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @Size(max = 200) String bookName,
        @Size(max = 150) String authorName) {
}
