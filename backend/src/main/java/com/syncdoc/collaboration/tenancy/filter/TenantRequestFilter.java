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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
            || path.startsWith("/actuator/")
            || path.startsWith("/api/v1/webhooks/")
            || path.startsWith("/api/v1/billing/")
            || path.startsWith("/ws/")
            || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            currentTenant.set(tenantId);
        }
        // If no workspace header, proceed without setting tenant context
        // (workspace-scoped endpoints enforce the header themselves via WorkspaceMembershipFilter)
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