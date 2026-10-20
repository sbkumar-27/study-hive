package com.studyhive.desktop.model;

import java.time.Instant;

public record User(
        Long userId,
        String username,
        String email,
        String displayName,
        GlobalRole globalRole,
        AccountStatus accountStatus,
        Instant createdAt) {
}
