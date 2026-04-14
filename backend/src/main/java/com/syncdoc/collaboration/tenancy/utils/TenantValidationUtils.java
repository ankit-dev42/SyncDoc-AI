package com.syncdoc.collaboration.tenancy.utils;

public final class TenantValidationUtils {

    private TenantValidationUtils() {
    }

    public static boolean isWorkspaceScopeValid(String workspaceIdFromPath, String workspaceIdFromHeader) {
        return workspaceIdFromPath != null
            && !workspaceIdFromPath.isBlank()
            && workspaceIdFromHeader != null
            && workspaceIdFromPath.equals(workspaceIdFromHeader);
    }

    public static boolean hasWorkspaceAccess(boolean isMember, String workspaceIdFromPath, String workspaceIdFromHeader) {
        return isMember && isWorkspaceScopeValid(workspaceIdFromPath, workspaceIdFromHeader);
    }
}
