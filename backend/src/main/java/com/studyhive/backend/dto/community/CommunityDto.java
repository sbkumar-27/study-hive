package com.studyhive.backend.dto.community;

import com.studyhive.backend.entity.Community;
import com.studyhive.backend.entity.CommunityStatus;
import com.studyhive.backend.entity.CommunityType;
import com.studyhive.backend.entity.MemberRole;
import java.time.Instant;

public record CommunityDto(
        Long communityId,
        String name,
        String description,
        CommunityType communityType,
        CommunityStatus status,
        // Only populated when the caller is a member (or the community is public); never leaks for a
        // private community to a non-member.
        String joinCode,
        Long ownerId,
        String ownerUsername,
        boolean hasPassword,
        long memberCount,
        MemberRole currentUserRole,
        Instant createdAt,
        // True when the caller is not a member but opened the community's share link: read-only access.
        boolean guestAccess) {

    public static CommunityDto of(
            Community community, long memberCount, MemberRole currentUserRole, boolean includeJoinCode) {
        return of(community, memberCount, currentUserRole, includeJoinCode, false);
    }

    public static CommunityDto of(
            Community community,
            long memberCount,
            MemberRole currentUserRole,
            boolean includeJoinCode,
            boolean guestAccess) {
        return new CommunityDto(
                community.getCommunityId(),
                community.getName(),
                community.getDescription(),
                community.getCommunityType(),
                community.getStatus(),
                includeJoinCode ? community.getJoinCode() : null,
                community.getOwner().getUserId(),
                community.getOwner().getUsername(),
                community.getPrivatePasswordHash() != null,
                memberCount,
                currentUserRole,
                community.getCreatedAt(),
                guestAccess);
    }
}
