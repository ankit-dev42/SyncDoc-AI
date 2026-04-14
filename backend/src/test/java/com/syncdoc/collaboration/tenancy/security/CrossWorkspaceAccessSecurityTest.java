package com.syncdoc.collaboration.tenancy.security;

import com.syncdoc.collaboration.tenancy.utils.TenantValidationUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrossWorkspaceAccessSecurityTest {

    @Test
    void shouldDenyWhenUserHasNoWorkspaceMembership() {
        boolean hasAccess = TenantValidationUtils.hasWorkspaceAccess(false, "ws-001", "ws-001");
        assertThat(hasAccess).isFalse();
    }

    @Test
    void shouldDenyWhenWorkspaceScopesDifferEvenWithMembership() {
        boolean hasAccess = TenantValidationUtils.hasWorkspaceAccess(true, "ws-001", "ws-002");
        assertThat(hasAccess).isFalse();
    }
}
