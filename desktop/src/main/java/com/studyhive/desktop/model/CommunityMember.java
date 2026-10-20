package com.studyhive.desktop.model;

import java.time.Instant;

public record CommunityMember(
        Long userId, String username, String displayName, MemberRole memberRole, Instant joinedAt) {
}
