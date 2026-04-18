package com.syncdoc.collaboration.ai.repository;

import com.syncdoc.collaboration.ai.model.GeneratedDocumentation;
import com.syncdoc.collaboration.ai.model.GeneratedDocumentation.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedDocumentationRepository extends JpaRepository<GeneratedDocumentation, String> {

    List<GeneratedDocumentation> findByUserId(String userId);

    List<GeneratedDocumentation> findByStatus(ProcessingStatus status);
}