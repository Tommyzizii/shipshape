package com.shipshape.identity.internal;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// The logged-in user as Spring Security sees it; stored in the HTTP session after login
public final class AppUserPrincipal implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private String passwordHash;

    private AppUserPrincipal(Long id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    static AppUserPrincipal from(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    public Long id() {
        return id;
    }

    String email() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // Called after a successful login, so the hash is never kept in the session
    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
