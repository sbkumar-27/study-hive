package com.studyhive.backend.common;

import com.studyhive.backend.common.AccountStatus;
import com.studyhive.backend.common.GlobalRole;
import com.studyhive.backend.common.User;
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
