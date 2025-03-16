package com.jobflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration
 * 
 * Configures web-related settings including CORS, interceptors, and resource handlers.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.max-age}")
    private long maxAge;

    /**
     * Configure CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins.split(","))
            .allowedMethods(allowedMethods.split(","))
            .allowedHeaders(allowedHeaders.split(","))
            .allowCredentials(true)
            .maxAge(maxAge);
    }

    /**
     * Configure resource handlers
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Swagger UI resources
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
            .resourceChain(false);

        // API docs resources
        registry.addResourceHandler("/api-docs/**")
            .addResourceLocations("classpath:/META-INF/resources/")
            .resourceChain(false);

        // Static resources
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(3600)
            .resourceChain(true);
    }

    /**
     * Configure interceptors
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add tenant interceptor
        registry.addInterceptor(tenantInterceptor())
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/**", "/api/public/**", 
                               "/swagger-ui/**", "/api-docs/**");

        // Add logging interceptor
        registry.addInterceptor(loggingInterceptor())
            .addPathPatterns("/api/**");

        // Add metrics interceptor
        registry.addInterceptor(metricsInterceptor())
            .addPathPatterns("/api/**");
    }

    /**
     * Tenant interceptor bean
     */
    @Bean
    public TenantInterceptor tenantInterceptor() {
        return new TenantInterceptor();
    }

    /**
     * Logging interceptor bean
     */
    @Bean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    /**
     * Metrics interceptor bean
     */
    @Bean
    public MetricsInterceptor metricsInterceptor() {
        return new MetricsInterceptor();
    }

    /**
     * Tenant interceptor class
     */
    private static class TenantInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler) {
            String tenantId = request.getHeader("X-Tenant-ID");
            if (tenantId != null) {
                TenantContext.setCurrentTenant(Long.parseLong(tenantId));
            }
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  Object handler, 
                                  Exception ex) {
            TenantContext.clear();
        }
    }

    /**
     * Logging interceptor class
     */
    private static class LoggingInterceptor implements HandlerInterceptor {
        private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

        @Override
        public boolean preHandle(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler) {
            log.info("Request: {} {}", request.getMethod(), request.getRequestURI());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  Object handler, 
                                  Exception ex) {
            log.info("Response: {} {} - {}", 
                request.getMethod(), 
                request.getRequestURI(), 
                response.getStatus());
        }
    }

    /**
     * Metrics interceptor class
     */
    private static class MetricsInterceptor implements HandlerInterceptor {
        private static final String REQUEST_START_TIME = "requestStartTime";

        @Override
        public boolean preHandle(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler) {
            request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  Object handler, 
                                  Exception ex) {
            Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                MetricsRegistry.recordRequestDuration(
                    request.getMethod(),
                    request.getRequestURI(),
                    duration
                );
            }
        }
    }

    /**
     * Tenant context holder
     */
    public static class TenantContext {
        private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

        public static void setCurrentTenant(Long tenantId) {
            currentTenant.set(tenantId);
        }

        public static Long getCurrentTenant() {
            return currentTenant.get();
        }

        public static void clear() {
            currentTenant.remove();
        }
    }

    /**
     * Metrics registry
     */
    public static class MetricsRegistry {
        private static final Counter requestCounter = Counter.build()
            .name("http_requests_total")
            .help("Total HTTP requests")
            .labelNames("method", "path")
            .register();

        private static final Histogram requestDuration = Histogram.build()
            .name("http_request_duration_seconds")
            .help("HTTP request duration in seconds")
            .labelNames("method", "path")
            .register();

        public static void recordRequestDuration(String method, String path, long durationMs) {
            requestCounter.labels(method, path).inc();
            requestDuration.labels(method, path).observe(durationMs / 1000.0);
        }
    }
}
