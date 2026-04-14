package com.syncdoc.collaboration.tenancy.security;

import com.syncdoc.collaboration.tenancy.utils.TenantValidationUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkspaceMembershipFilter extends OncePerRequestFilter {

    private static final Pattern WORKSPACE_PATH_PATTERN = Pattern.compile("/api/v1/workspaces/([^/]+)/.*");
    private static final String WORKSPACE_HEADER = "X-Workspace-Id";

    private final WorkspaceMembershipService workspaceMembershipService;

    public WorkspaceMembershipFilter(WorkspaceMembershipService workspaceMembershipService) {
        this.workspaceMembershipService = workspaceMembershipService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !WORKSPACE_PATH_PATTERN.matcher(request.getRequestURI()).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Matcher matcher = WORKSPACE_PATH_PATTERN.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        String workspaceIdFromPath = matcher.group(1);
        String workspaceIdFromHeader = request.getHeader(WORKSPACE_HEADER);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
            return;
        }

        String userId = String.valueOf(authentication.getPrincipal());
        boolean memberHasAccess = workspaceMembershipService.isMember(workspaceIdFromPath, userId);

        if (!TenantValidationUtils.hasWorkspaceAccess(memberHasAccess, workspaceIdFromPath, workspaceIdFromHeader)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Workspace membership validation failed");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
