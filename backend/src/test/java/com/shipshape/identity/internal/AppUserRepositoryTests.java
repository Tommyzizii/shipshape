package com.shipshape.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.shipshape.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AppUserRepositoryTests {

    @Autowired
    AppUserRepository repository;

    @Test
    void storesEmailNormalized() {
        repository.saveAndFlush(new AppUser("  Tommy@Example.COM ", "hash"));

        var found = repository.findByEmail("tommy@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("tommy@example.com");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        repository.saveAndFlush(new AppUser("dup@example.com", "hash"));

        assertThatThrownBy(() -> repository.saveAndFlush(new AppUser("DUP@example.com", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}