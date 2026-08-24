package lumi.insert.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger OpenAPI Config.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("LUMI Insert Java Edition API")
                .version("1.0.0")
                .description("Comprehensive REST API for LUMI Insert inventory management system. Handles categories, products, employees, customers, transactions, supplies, and more.")
                .contact(new io.swagger.v3.oas.models.info.Contact()
                    .name("LUMI Insert Support")
                    .email("support@lumi-insert.my.id"))
                .license(new License()
                    .name("Licensed")
                    .url("https://www.lumi-insert.my.id")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token for authentication. Obtained from /api/auth/login endpoint"))
                .addResponses("NotFound", new ApiResponse()
                    .description("Resource not found")
                    .content(new Content()
                        .addMediaType("application/json",
                            new MediaType()
                                .schema(new Schema<>()
                                    .type("object")
                                    .addProperty("errors", new Schema<>().type("string").example("Entity not found"))))))
                .addResponses("BadRequest", new ApiResponse()
                    .description("Invalid input request")
                    .content(new Content()
                        .addMediaType("application/json",
                            new MediaType()
                                .schema(new Schema<>()
                                    .type("object")
                                    .addProperty("errors", new Schema<>().type("string").example("Validation failed"))))))
                .addResponses("Unauthorized", new ApiResponse()
                    .description("Authentication required")
                    .content(new Content()
                        .addMediaType("application/json",
                            new MediaType()
                                .schema(new Schema<>()
                                    .type("object")
                                    .addProperty("errors", new Schema<>().type("string").example("Authorization token missing"))))))
                .addResponses("Forbidden", new ApiResponse()
                    .description("Access denied - insufficient permissions")
                    .content(new Content()
                        .addMediaType("application/json",
                            new MediaType()
                                .schema(new Schema<>()
                                    .type("object")
                                    .addProperty("errors", new Schema<>().type("string").example("Access forbidden"))))))
                .addResponses("ServerError", new ApiResponse()
                    .description("Internal server error")
                    .content(new Content()
                        .addMediaType("application/json",
                            new MediaType()
                                .schema(new Schema<>()
                                    .type("object")
                                    .addProperty("errors", new Schema<>().type("string").example("Internal server error"))))))
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
