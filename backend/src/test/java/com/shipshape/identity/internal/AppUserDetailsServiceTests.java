package com.shipshape.identity.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import com.shipshape.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AppUserDetailsServiceTests {

    @Autowired
    AppUserDetailsService userDetailsService;

    @Autowired
    AppUserRepository users;

    @Test
    void loadsUserByNormalizedEmail() {
        AppUser saved = users.saveAndFlush(new AppUser("tommy@example.com", "{bcrypt}hash"));

        var principal = (AppUserPrincipal) userDetailsService.loadUserByUsername("  Tommy@Example.com ");

        assertThat(principal.id()).isEqualTo(saved.getId());
        assertThat(principal.getUsername()).isEqualTo("tommy@example.com");
        assertThat(principal.getPassword()).isEqualTo("{bcrypt}hash");
    }

    @Test
    void unknownEmailThrowsUsernameNotFound() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
