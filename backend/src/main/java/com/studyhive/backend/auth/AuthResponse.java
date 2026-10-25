package com.studyhive.backend.auth;

import com.studyhive.backend.common.UserDto;

public record AuthResponse(String token, UserDto user) {
}
