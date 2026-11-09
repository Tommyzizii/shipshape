package com.shipshape.identity.internal;

record UserResponse(Long id, String email) {

    static UserResponse of(AppUserPrincipal principal) {
        return new UserResponse(principal.id(), principal.email());
    }
}
