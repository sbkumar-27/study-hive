package com.studyhive.backend.communities;

import jakarta.validation.constraints.NotBlank;

public record JoinCommunityRequest(@NotBlank String joinCode, String password) {
}
