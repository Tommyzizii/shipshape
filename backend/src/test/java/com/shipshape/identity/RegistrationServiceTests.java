package com.shipshape.identity;

import com.shipshape.TestcontainersConfiguration;
import com.shipshape.identity.internal.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.shipshape.identity.EmailAlreadyTakenException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RegistrationServiceTests {

    @Autowired
    RegistrationService registrationService;

    @Autowired
    AppUserRepository repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void registersUserWithHashedPassword() {
        String raw = "correct-horse-battery";

        RegisteredUser result = registrationService.register("  New@Example.com ", raw);

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.id()).isNotNull();

        var stored = repository.findByEmail("new@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo(raw);
        assertThat(passwordEncoder.matches(raw, stored.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        registrationService.register("taken@example.com", "correct-horse-battery");

        assertThatThrownBy(() -> registrationService.register("TAKEN@example.com", "another-password-1"))
                .isInstanceOf(EmailAlreadyTakenException.class);
    }
}