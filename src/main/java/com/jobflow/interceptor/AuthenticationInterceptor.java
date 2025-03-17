package com.jobflow.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobflow.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    private static final String[] PUBLIC_PATHS = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot-password",
        "/error"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip authentication for public paths
        String requestPath = request.getRequestURI();
        for (String publicPath : PUBLIC_PATHS) {
            if (requestPath.startsWith(publicPath)) {
                return true;
            }
        }

        // Check for static resources
        if (requestPath.startsWith("/static/") || 
            requestPath.startsWith("/css/") || 
            requestPath.startsWith("/js/") || 
            requestPath.startsWith("/images/") || 
            requestPath.startsWith("/lib/")) {
            return true;
        }

        // Get token from header
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "Missing or invalid token");
            return false;
        }

        token = token.substring(7); // Remove "Bearer " prefix

        try {
            // Validate token
            if (!jwtUtils.validateToken(token)) {
                sendUnauthorizedResponse(response, "Invalid token");
                return false;
            }

            // Set user info in request attributes
            Long userId = jwtUtils.getUserIdFromToken(token);
            Long tenantId = jwtUtils.getTenantIdFromToken(token);
            request.setAttribute("userId", userId);
            request.setAttribute("tenantId", tenantId);

            return true;
        } catch (Exception e) {
            sendUnauthorizedResponse(response, "Token validation failed");
            return false;
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new HashMap<>();
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", message);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
