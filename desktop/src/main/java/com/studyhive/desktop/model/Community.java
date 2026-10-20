package com.studyhive.desktop.model;

import java.time.Instant;

public record Community(
        Long communityId,
        String name,
        String description,
        CommunityType communityType,
        CommunityStatus status,
        String joinCode,
        Long ownerId,
        String ownerUsername,
        boolean hasPassword,
        long memberCount,
        MemberRole currentUserRole,
        Instant createdAt) {
}
