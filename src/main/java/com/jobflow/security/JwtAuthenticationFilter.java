package com.jobflow.security;

import com.jobflow.domain.User;
import com.jobflow.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT Authentication Filter
 * 
 * Intercepts requests and validates JWT tokens.
 * Sets the authentication in the SecurityContext if valid.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserService userService) {
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromToken(jwt);
                Long tenantId = tokenProvider.getTenantIdFromToken(jwt);

                User user = userService.findById(userId);
                if (user != null && user.getTenantId().equals(tenantId)) {
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Check if token needs to be refreshed
                    long remainingTime = tokenProvider.getTokenRemainingValidityTime(jwt);
                    if (shouldRefreshToken(remainingTime)) {
                        String newToken = tokenProvider.refreshToken(jwt);
                        response.setHeader("X-New-Token", newToken);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from request
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Check if token should be refreshed
     * Refresh if less than 30 minutes remaining
     */
    private boolean shouldRefreshToken(long remainingTime) {
        return remainingTime > 0 && remainingTime < 30 * 60 * 1000;
    }

    /**
     * Check if request should be filtered
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/") || 
               path.startsWith("/api/public/") ||
               path.equals("/api-docs") ||
               path.startsWith("/swagger-ui/") ||
               path.equals("/actuator/health");
    }

    /**
     * Handle authentication failure
     */
    private void handleAuthenticationFailure(HttpServletResponse response, String message) 
            throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }

    /**
     * Extract tenant ID from request
     * First tries header, then parameter
     */
    private Long getTenantIdFromRequest(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-ID");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getParameter("tenantId");
        }
        return StringUtils.hasText(tenantId) ? Long.parseLong(tenantId) : null;
    }

    /**
     * Validate tenant context
     * Ensures user belongs to the requested tenant
     */
    private boolean validateTenantContext(User user, HttpServletRequest request) {
        Long requestTenantId = getTenantIdFromRequest(request);
        return requestTenantId == null || requestTenantId.equals(user.getTenantId());
    }

    /**
     * Check if path requires tenant context
     */
    private boolean requiresTenantContext(String path) {
        return !path.startsWith("/api/auth/") && 
               !path.startsWith("/api/public/") &&
               !path.equals("/api-docs") &&
               !path.startsWith("/swagger-ui/") &&
               !path.equals("/actuator/health");
    }
}
