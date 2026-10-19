package com.studyhive.backend.dto.community;

import com.studyhive.backend.entity.CommunityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        CommunityType communityType,
        @Size(min = 4, max = 100) String privatePassword) {
}
