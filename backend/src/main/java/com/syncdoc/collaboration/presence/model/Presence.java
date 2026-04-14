package com.syncdoc.collaboration.presence.model;

import com.syncdoc.collaboration.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "presence", indexes = {
    @Index(name = "idx_presence_workspace_user", columnList = "workspace_id, user_id", unique = true),
    @Index(name = "idx_presence_workspace_status", columnList = "workspace_id, status")
})
public class Presence extends BaseEntity {

    public enum Status {
        ONLINE,
        AWAY,
        OFFLINE
    }

    @Column(name = "workspace_id", nullable = false, length = 50)
    private String workspaceId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status = Status.OFFLINE;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "manual_status")
    @Enumerated(EnumType.STRING)
    private Status manualStatus;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Status getManualStatus() {
        return manualStatus;
    }

    public void setManualStatus(Status manualStatus) {
        this.manualStatus = manualStatus;
    }
}
