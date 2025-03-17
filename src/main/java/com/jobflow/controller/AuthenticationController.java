package com.jobflow.controller;

import com.jobflow.dao.UserDao;
import com.jobflow.domain.User;
import com.jobflow.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserDao userDao;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Find user by username
        User user = userDao.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify password
        if (!verifyPassword(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Generate token
        String token = jwtUtils.generateToken(user.getId(), user.getTenantId(), user.getUsername());

        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("tenantId", user.getTenantId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Check if username exists
        if (userDao.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(hashPassword(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setTenantId(request.getTenantId());
        
        user = userDao.save(user);

        // Generate token
        String token = jwtUtils.generateToken(user.getId(), user.getTenantId(), user.getUsername());

        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("tenantId", user.getTenantId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        // Validate current token
        if (!jwtUtils.validateToken(request.getToken())) {
            throw new RuntimeException("Invalid token");
        }

        // Get user info from token
        Long userId = jwtUtils.getUserIdFromToken(request.getToken());
        Long tenantId = jwtUtils.getTenantIdFromToken(request.getToken());
        String username = jwtUtils.getUsernameFromToken(request.getToken());

        // Generate new token
        String newToken = jwtUtils.generateToken(userId, tenantId, username);

        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);

        return ResponseEntity.ok(response);
    }

    private boolean verifyPassword(String rawPassword, String hashedPassword) {
        // TODO: Implement proper password hashing and verification
        return rawPassword.equals(hashedPassword);
    }

    private String hashPassword(String password) {
        // TODO: Implement proper password hashing
        return password;
    }

    // Request/Response classes
    public static class LoginRequest {
        private String username;
        private String password;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private Long tenantId;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    }

    public static class RefreshTokenRequest {
        private String token;

        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
