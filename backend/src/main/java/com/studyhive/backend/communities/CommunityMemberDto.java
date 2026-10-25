package com.studyhive.backend.communities;

import com.studyhive.backend.communities.CommunityMember;
import com.studyhive.backend.communities.MemberRole;
import java.time.Instant;

public record CommunityMemberDto(
        Long userId, String username, String displayName, MemberRole memberRole, Instant joinedAt) {

    public static CommunityMemberDto from(CommunityMember member) {
        return new CommunityMemberDto(
                member.getUser().getUserId(),
                member.getUser().getUsername(),
                member.getUser().getDisplayName(),
                member.getMemberRole(),
                member.getJoinedAt());
    }
}
