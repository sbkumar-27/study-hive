package com.studyhive.backend.communities;

import java.io.Serializable;
import java.util.Objects;

public class CommunityMemberId implements Serializable {

    private Long community;
    private Long user;

    public CommunityMemberId() {
    }

    public CommunityMemberId(Long community, Long user) {
        this.community = community;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommunityMemberId that)) return false;
        return Objects.equals(community, that.community) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(community, user);
    }
}
