package com.studyhive.backend.dto.community;

import com.studyhive.backend.entity.Community;
import com.studyhive.backend.entity.CommunityType;

/** What someone sees when they open a share link, before they have access. */
public record ShareLinkPreviewDto(
        Long communityId,
        String name,
        String description,
        CommunityType communityType,
        String ownerUsername,
        long memberCount,
        boolean requiresPassword,
        boolean isMember,
        boolean hasAccess) {

    public static ShareLinkPreviewDto of(
            Community community, long memberCount, boolean isMember, boolean hasAccess) {
        return new ShareLinkPreviewDto(
                community.getCommunityId(),
                community.getName(),
                community.getDescription(),
                community.getCommunityType(),
                community.getOwner().getUsername(),
                memberCount,
                community.getPrivatePasswordHash() != null,
                isMember,
                hasAccess);
    }
}
