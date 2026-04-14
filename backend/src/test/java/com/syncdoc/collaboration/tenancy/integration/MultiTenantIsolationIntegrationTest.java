package com.syncdoc.collaboration.tenancy.integration;

import com.syncdoc.collaboration.tenancy.utils.TenantValidationUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiTenantIsolationIntegrationTest {

    @Test
    void shouldRejectCrossWorkspacePathHeaderMismatch() {
        boolean allowed = TenantValidationUtils.isWorkspaceScopeValid("ws-a", "ws-b");
        assertThat(allowed).isFalse();
    }

    @Test
    void shouldAllowMatchingWorkspacePathAndHeader() {
        boolean allowed = TenantValidationUtils.isWorkspaceScopeValid("ws-a", "ws-a");
        assertThat(allowed).isTrue();
    }
}
