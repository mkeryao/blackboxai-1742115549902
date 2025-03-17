package com.jobflow.config;

import com.jobflow.interceptor.AuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/",
                "/error",
                "/favicon.ico",
                "/static/**",
                "/css/**",
                "/js/**",
                "/images/**",
                "/lib/**",
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/forgot-password"
            );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Authorization")
            .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/lib/**")
            .addResourceLocations("classpath:/static/lib/");
    }
}
