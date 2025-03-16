package com.jobflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger Configuration
 * 
 * Configures OpenAPI documentation for the application.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
            .info(new Info()
                .title("JobFlow API")
                .description("API documentation for JobFlow - A distributed task scheduling and workflow management system")
                .version("1.0.0")
                .contact(new Contact()
                    .name("JobFlow Team")
                    .email("support@jobflow.com")
                    .url("https://jobflow.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement()
                .addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter JWT token")));
    }

    /**
     * Customize OpenAPI groups
     */
    @Bean
    public GroupedOpenApi authenticationApi() {
        return GroupedOpenApi.builder()
            .group("Authentication")
            .pathsToMatch("/api/auth/**")
            .build();
    }

    @Bean
    public GroupedOpenApi taskApi() {
        return GroupedOpenApi.builder()
            .group("Task Management")
            .pathsToMatch("/api/tasks/**")
            .build();
    }

    @Bean
    public GroupedOpenApi workflowApi() {
        return GroupedOpenApi.builder()
            .group("Workflow Management")
            .pathsToMatch("/api/workflows/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("User Management")
            .pathsToMatch("/api/users/**")
            .build();
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
            .group("Notification Management")
            .pathsToMatch("/api/notifications/**")
            .build();
    }

    @Bean
    public GroupedOpenApi operationLogApi() {
        return GroupedOpenApi.builder()
            .group("Operation Log Management")
            .pathsToMatch("/api/logs/**")
            .build();
    }

    /**
     * Customize Swagger UI
     */
    @Bean
    public SwaggerUiConfigParameters swaggerUiConfig() {
        return new SwaggerUiConfigParameters()
            .displayRequestDuration(true)
            .filter(true)
            .displayOperationId(false)
            .defaultModelsExpandDepth(1)
            .defaultModelExpandDepth(1)
            .defaultModelRendering(ModelRendering.EXAMPLE)
            .displayRequestDuration(true)
            .docExpansion(DocExpansion.NONE)
            .showExtensions(false)
            .showCommonExtensions(false)
            .supportedSubmitMethods(UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS)
            .validatorUrl(null);
    }

    /**
     * Customize OpenAPI operation customizer
     */
    @Bean
    public OpenApiCustomiser openApiCustomiser() {
        return openApi -> openApi.getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .forEach(operation -> {
                // Add common parameters
                operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Tenant-ID")
                    .description("Tenant ID")
                    .schema(new Schema<String>().type("string"))
                    .required(false));

                // Add common responses
                operation.getResponses()
                    .addApiResponse("401", new ApiResponse()
                        .description("Unauthorized"))
                    .addApiResponse("403", new ApiResponse()
                        .description("Forbidden"))
                    .addApiResponse("500", new ApiResponse()
                        .description("Internal Server Error"));
            });
    }
}
