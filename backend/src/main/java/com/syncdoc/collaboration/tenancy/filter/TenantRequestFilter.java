package com.syncdoc.collaboration.tenancy.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantRequestFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Workspace-Id";
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            currentTenant.set(tenantId);
        } else {
            // For MVP, assume default or throw error
            throw new IllegalArgumentException("Missing workspace ID");
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            currentTenant.remove();
        }
    }

    public static String getCurrentTenantId() {
        return currentTenant.get();
    }
}