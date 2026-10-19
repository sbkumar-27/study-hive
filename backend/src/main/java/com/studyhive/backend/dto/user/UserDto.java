package com.studyhive.backend.dto.user;

import com.studyhive.backend.entity.AccountStatus;
import com.studyhive.backend.entity.GlobalRole;
import com.studyhive.backend.entity.User;
import java.time.Instant;

public record UserDto(
        Long userId,
        String username,
        String email,
        String displayName,
        GlobalRole globalRole,
        AccountStatus accountStatus,
        Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getGlobalRole(),
                user.getAccountStatus(),
                user.getCreatedAt());
    }
}
