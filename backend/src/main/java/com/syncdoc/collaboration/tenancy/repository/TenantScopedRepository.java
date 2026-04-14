package com.syncdoc.collaboration.tenancy.repository;

import com.syncdoc.collaboration.tenancy.filter.TenantRequestFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface TenantScopedRepository<T, ID> extends JpaRepository<T, ID> {

    default String getCurrentTenantId() {
        return TenantRequestFilter.getCurrentTenantId();
    }

    // Override findAll to filter by tenant
    // But in practice, each repo will have custom queries with @Query("... where workspaceId = :tenantId")

    // Example method
    // List<T> findByWorkspaceId(String workspaceId);
}