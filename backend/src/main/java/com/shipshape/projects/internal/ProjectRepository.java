package com.shipshape.projects.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// Every query that serves a user takes ownerId, so another user's project can never be returned
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
}
