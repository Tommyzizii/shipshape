package com.shipshape.projects.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.shipshape.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ProjectRepositoryTests {

    @Autowired
    ProjectRepository projects;

    @Autowired
    JdbcTemplate jdbc;

    Long alice;
    Long bob;

    @BeforeEach
    void createOwners() {
        alice = insertUser("alice@example.com");
        bob = insertUser("bob@example.com");
    }

    @Test
    void ownerOnlySeesOwnProjects() {
        Project alicesProject = projects.saveAndFlush(new Project(alice, "Alice API"));
        projects.saveAndFlush(new Project(bob, "Bob API"));

        assertThat(projects.findAllByOwnerIdOrderByNameAsc(bob))
                .extracting(Project::getName)
                .containsExactly("Bob API");
        assertThat(projects.findByIdAndOwnerId(alicesProject.getId(), bob)).isEmpty();
        assertThat(projects.findByIdAndOwnerId(alicesProject.getId(), alice)).isPresent();
    }

    @Test
    void rejectsDuplicateNameForSameOwnerIgnoringCase() {
        projects.saveAndFlush(new Project(alice, "My API"));

        assertThatThrownBy(() -> projects.saveAndFlush(new Project(alice, "my api")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameNameForDifferentOwners() {
        projects.saveAndFlush(new Project(alice, "My API"));
        projects.saveAndFlush(new Project(bob, "My API"));

        assertThat(projects.findAllByOwnerIdOrderByNameAsc(bob))
                .extracting(Project::getName)
                .containsExactly("My API");
    }

    @Test
    void rejectsNameLongerThan100Characters() {
        assertThatThrownBy(() -> projects.saveAndFlush(new Project(alice, "x".repeat(101))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsUnknownOwner() {
        assertThatThrownBy(() -> projects.saveAndFlush(new Project(-1L, "Orphan")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingOwnerDeletesTheirProjects() {
        projects.saveAndFlush(new Project(alice, "Alice API"));

        jdbc.update("delete from app_user where id = ?", alice);

        // Ask the database directly: the JPA persistence context would still remember the deleted project
        Integer remaining = jdbc.queryForObject("select count(*) from project where owner_id = ?", Integer.class, alice);
        assertThat(remaining).isZero();
    }

    // Plain SQL instead of identity's AppUserRepository: the projects module must not depend on identity internals
    private Long insertUser(String email) {
        return jdbc.queryForObject(
                "insert into app_user (email, password_hash) values (?, 'hash') returning id", Long.class, email);
    }
}
