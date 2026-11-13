package com.quickkart.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI quickKartOpenAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Quick kart Fashion E-Commerce API")
                        .description("Full-stack REST API for Quick kart Customer Storefront and Shop Owner Admin Dashboard")
                        .version("1.0.0")
                        .contact(new Contact().name("Quick kart Support").email("support@quickkart.com")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
