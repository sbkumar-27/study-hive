package com.studyhive.backend.security;

import com.studyhive.backend.entity.AccountStatus;
import com.studyhive.backend.entity.GlobalRole;
import com.studyhive.backend.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * UserDetails backed directly by a User entity, so controllers can pull the
 * real userId/globalRole off the security principal instead of re-querying.
 */
public class AppUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final GlobalRole globalRole;
    private final boolean enabled;

    public AppUserPrincipal(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.globalRole = user.getGlobalRole();
        this.enabled = user.getAccountStatus() == AccountStatus.ACTIVE;
    }

    public Long getUserId() {
        return userId;
    }

    public GlobalRole getGlobalRole() {
        return globalRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + globalRole.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
