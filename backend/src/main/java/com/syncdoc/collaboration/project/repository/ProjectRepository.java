package com.syncdoc.collaboration.project.repository;

import com.syncdoc.collaboration.project.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, String> {

    List<Project> findByOwnerId(UUID ownerId);

    Page<Project> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);
}
