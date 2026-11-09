package com.shipshape.identity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.shipshape.identity.internal.AppUserPrincipal;

// Front desk for other modules: who is making this request? Never expose Spring Security types or AppUser.
@Component
public class CurrentUser {

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("No authenticated user");
        }
        return principal.id();
    }
}
