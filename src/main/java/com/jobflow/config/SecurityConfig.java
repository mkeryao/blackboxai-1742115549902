package com.jobflow.config;

import com.jobflow.security.JwtAuthenticationFilter;
import com.jobflow.security.JwtTokenProvider;
import com.jobflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Security Configuration
 * 
 * Configures Spring Security with JWT authentication and role-based authorization.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true,
    prePostEnabled = true
)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userService);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService)
            .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors()
                .and()
            .csrf()
                .disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            .authorizeRequests()
                // Public endpoints
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                .antMatchers("/actuator/health").permitAll()
                // Admin endpoints
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                // Task management endpoints
                .antMatchers("/api/tasks/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .antMatchers("/api/workflows/**").hasAnyRole("ADMIN", "MANAGER")
                // User management endpoints
                .antMatchers("/api/users/**").hasAnyRole("ADMIN", "MANAGER")
                // Monitoring endpoints
                .antMatchers("/actuator/**").hasRole("ADMIN")
                // All other endpoints require authentication
                .anyRequest().authenticated()
                .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow all origins for development
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L); // 1 hour
        
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

    /**
     * Configure security for WebSocket endpoints
     */
    @Bean
    public WebSocketSecurityConfig webSocketSecurityConfig() {
        return new WebSocketSecurityConfig() {
            @Override
            protected void configureInbound(MessageSecurityMetadata.Builder messages) {
                messages
                    .simpDestMatchers("/topic/public/**").permitAll()
                    .simpDestMatchers("/topic/private/**").authenticated()
                    .simpDestMatchers("/app/**").authenticated()
                    .anyMessage().authenticated();
            }
        };
    }

    /**
     * Configure method-level security
     */
    @Bean
    public MethodSecurityConfig methodSecurityConfig() {
        return new MethodSecurityConfig() {
            @Override
            protected void configure(GlobalMethodSecurityConfiguration configuration) {
                configuration.setPrePostEnabled(true);
                configuration.setSecuredEnabled(true);
                configuration.setJsr250Enabled(true);
            }
        };
    }
}
