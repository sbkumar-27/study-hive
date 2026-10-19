package com.studyhive.backend.dto.community;

import jakarta.validation.constraints.NotBlank;

public record JoinCommunityRequest(@NotBlank String joinCode, String password) {
}
