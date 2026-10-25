package com.studyhive.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String usernameOrEmail, @NotBlank String password) {
}
