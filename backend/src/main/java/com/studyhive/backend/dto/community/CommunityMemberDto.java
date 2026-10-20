package com.studyhive.backend.dto.community;

import com.studyhive.backend.entity.CommunityMember;
import com.studyhive.backend.entity.MemberRole;
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
