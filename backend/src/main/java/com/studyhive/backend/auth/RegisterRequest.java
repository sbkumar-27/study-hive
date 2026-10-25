package com.studyhive.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(
                regexp = "^[a-zA-Z0-9_.-]+$",
                message = "must contain only letters, numbers, dots, dashes, or underscores")
                String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(min = 1, max = 100) String displayName) {
}
