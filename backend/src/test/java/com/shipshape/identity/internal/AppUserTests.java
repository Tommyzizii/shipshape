package com.shipshape.identity.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTests {

    @Test
    void normalizeEmailStripsUnicodeWhitespace() {
        String normalized = AppUser.normalizeEmail(" New@Example.com　");

        assertThat(normalized).isEqualTo("new@example.com");
    }
}
