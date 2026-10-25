package com.studyhive.backend.communities;

import java.io.Serializable;
import java.util.Objects;

public class CommunityGuestAccessId implements Serializable {

    private Long community;
    private Long user;

    public CommunityGuestAccessId() {
    }

    public CommunityGuestAccessId(Long community, Long user) {
        this.community = community;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommunityGuestAccessId that)) return false;
        return Objects.equals(community, that.community) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(community, user);
    }
}
