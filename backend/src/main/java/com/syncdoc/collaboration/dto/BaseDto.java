package com.syncdoc.collaboration.dto;

import jakarta.validation.constraints.NotNull;

public abstract class BaseDto {

    @NotNull
    protected String workspaceId;

    // Getters and setters
    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }
}