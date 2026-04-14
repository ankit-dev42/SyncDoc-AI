package com.syncdoc.collaboration.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "generated_documentation", indexes = {
    @Index(name = "idx_generated_doc_user", columnList = "user_id"),
    @Index(name = "idx_generated_doc_status", columnList = "status")
})
public class GeneratedDocumentation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "source_content_id", nullable = false, length = 50)
    private String sourceContentId;

    @Column(name = "source_content", columnDefinition = "TEXT")
    private String sourceContent;

    @Column(name = "key_changes", columnDefinition = "TEXT")
    private String keyChanges;

    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public GeneratedDocumentation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSourceContentId() {
        return sourceContentId;
    }

    public void setSourceContentId(String sourceContentId) {
        this.sourceContentId = sourceContentId;
    }

    public String getSourceContent() {
        return sourceContent;
    }

    public void setSourceContent(String sourceContent) {
        this.sourceContent = sourceContent;
    }

    public String getKeyChanges() {
        return keyChanges;
    }

    public void setKeyChanges(String keyChanges) {
        this.keyChanges = keyChanges;
    }

    public String getActionItems() {
        return actionItems;
    }

    public void setActionItems(String actionItems) {
        this.actionItems = actionItems;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public enum ProcessingStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}