package com.studyhive.backend.communities;

import com.studyhive.backend.common.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/** Read-only access to a community granted through its share link, without membership. */
@Entity
@Table(name = "community_guest_access")
@IdClass(CommunityGuestAccessId.class)
public class CommunityGuestAccess {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    public CommunityGuestAccess() {
    }

    public CommunityGuestAccess(Community community, User user) {
        this.community = community;
        this.user = user;
    }

    public Community getCommunity() {
        return community;
    }

    public User getUser() {
        return user;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }
}
