package com.studyhive.backend.communities;

import java.time.Instant;

public record CommunityGuestDto(Long userId, String username, Instant grantedAt) {

    public static CommunityGuestDto from(CommunityGuestAccess guestAccess) {
        return new CommunityGuestDto(
                guestAccess.getUser().getUserId(),
                guestAccess.getUser().getUsername(),
                guestAccess.getGrantedAt());
    }
}
