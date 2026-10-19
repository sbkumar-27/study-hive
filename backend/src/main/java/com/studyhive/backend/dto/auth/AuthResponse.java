package com.studyhive.backend.dto.auth;

import com.studyhive.backend.dto.user.UserDto;

public record AuthResponse(String token, UserDto user) {
}
