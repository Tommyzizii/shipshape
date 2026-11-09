package com.shipshape.identity.internal;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(@NotBlank String email, @NotBlank String password) {

    LoginRequest {
        if (email != null) {
            email = email.strip();
        }
    }
}
