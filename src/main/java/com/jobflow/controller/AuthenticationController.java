package com.jobflow.controller;

import com.jobflow.domain.User;
import com.jobflow.security.JwtTokenProvider;
import com.jobflow.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 * 
 * Handles user authentication and token management endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager,
                                  JwtTokenProvider tokenProvider,
                                  UserService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();
            String jwt = tokenProvider.generateToken(authentication);

            // Update last login information
            userService.updateLoginSuccess(user.getId(), loginRequest.getIpAddress(), "system");

            return ResponseEntity.ok(new LoginResponse(jwt, user));
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Invalid username or password"));
        }
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                if (tokenProvider.validateToken(jwt)) {
                    String refreshedToken = tokenProvider.refreshToken(jwt);
                    return ResponseEntity.ok(new TokenResponse(refreshedToken));
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Invalid token"));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Token refresh failed"));
        }
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    /**
     * Validate token endpoint
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                boolean isValid = tokenProvider.validateToken(jwt);
                return ResponseEntity.ok(new ValidationResponse(isValid));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Invalid token"));
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Token validation failed"));
        }
    }

    /**
     * Get current user endpoint
     */
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
            return ResponseEntity.ok(currentUser);
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Not authenticated"));
        }
    }

    // Request/Response classes

    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String ipAddress;
    }

    @lombok.Data
    public static class LoginResponse {
        private String token;
        private Map<String, Object> user;

        public LoginResponse(String token, User user) {
            this.token = token;
            this.user = new HashMap<>();
            this.user.put("id", user.getId());
            this.user.put("username", user.getUsername());
            this.user.put("tenantId", user.getTenantId());
            this.user.put("roles", user.getRoles());
            this.user.put("email", user.getEmail());
            this.user.put("status", user.getStatus());
        }
    }

    @lombok.Data
    public static class TokenResponse {
        private String token;

        public TokenResponse(String token) {
            this.token = token;
        }
    }

    @lombok.Data
    public static class ValidationResponse {
        private boolean valid;

        public ValidationResponse(boolean valid) {
            this.valid = valid;
        }
    }

    @lombok.Data
    public static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }

    @lombok.Data
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
