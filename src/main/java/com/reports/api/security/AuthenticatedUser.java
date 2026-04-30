package com.reports.api.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedUser extends AbstractAuthenticationToken {
    private final UUID userId;

    public AuthenticatedUser(UUID userId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public static AuthenticatedUser withoutAuthorities(UUID userId) {
        return new AuthenticatedUser(userId, List.of());
    }
}
