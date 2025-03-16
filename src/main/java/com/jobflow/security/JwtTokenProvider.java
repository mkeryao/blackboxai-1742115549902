package com.jobflow.security;

import com.jobflow.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Provider
 * 
 * Handles JWT token generation, validation, and parsing.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpirationInMs;

    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate token from authentication object
     */
    public String generateToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();
        return generateToken(userPrincipal);
    }

    /**
     * Generate token from user object
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("tenantId", user.getTenantId());
        claims.put("roles", user.getRoles());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getId().toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Get username from token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Get tenant ID from token
     */
    public Long getTenantIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("tenantId", Long.class);
    }

    /**
     * Get roles from token
     */
    @SuppressWarnings("unchecked")
    public String getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", String.class);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Parse token and return claims
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Get token expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Get remaining validity time in milliseconds
     */
    public long getTokenRemainingValidityTime(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * Refresh token
     */
    public String refreshToken(String token) {
        Claims claims = parseToken(token);
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    /**
     * Create token with custom expiration time
     */
    public String generateToken(User user, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("tenantId", user.getTenantId());
        claims.put("roles", user.getRoles());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getId().toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }
}
