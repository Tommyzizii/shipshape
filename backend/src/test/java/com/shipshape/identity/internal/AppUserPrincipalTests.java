package com.shipshape.identity.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserPrincipalTests {

    @Test
    void eraseCredentialsRemovesPasswordHash() {
        AppUserPrincipal principal = AppUserPrincipal.from(new AppUser("tommy@example.com", "{bcrypt}hash"));

        principal.eraseCredentials();

        assertThat(principal.getPassword()).isNull();
        assertThat(principal.getUsername()).isEqualTo("tommy@example.com");
    }
}
