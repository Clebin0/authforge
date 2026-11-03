package dev.clebin.authforge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI authForgeOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("AuthForge API")
                .version("1.0.0")
                .description("API para cadastro, autenticação e administração de contas. Usa JWT de curta duração e refresh tokens com rotação.")
                .contact(new Contact().name("Clebin0").url("https://github.com/Clebin0")))
            .components(new Components().addSecuritySchemes(
                "bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            ));
    }
}
