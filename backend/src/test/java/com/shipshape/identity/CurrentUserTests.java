package com.shipshape.identity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

import com.shipshape.TestcontainersConfiguration;
import com.shipshape.identity.internal.AppUser;
import com.shipshape.identity.internal.AppUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CurrentUserTests {

    @Autowired
    CurrentUser currentUser;

    @Autowired
    AppUserRepository users;

    @Autowired
    UserDetailsService userDetailsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsIdOfLoggedInUser() {
        AppUser saved = users.saveAndFlush(new AppUser("tommy@example.com", "{bcrypt}hash"));
        UserDetails principal = userDetailsService.loadUserByUsername("tommy@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        assertThat(currentUser.id()).isEqualTo(saved.getId());
    }

    @Test
    void throwsWhenNobodyIsLoggedIn() {
        assertThatThrownBy(() -> currentUser.id())
                .isInstanceOf(IllegalStateException.class);
    }
}
