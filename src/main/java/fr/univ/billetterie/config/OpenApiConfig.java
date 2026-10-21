package fr.univ.billetterie.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String NOM_SCHEMA_SECURITE = "AuthentificationBasic";

    @Bean
    public OpenAPI configurerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Billetterie Cinéma")
                        .version("1.0")
                        .description("API REST du backend de billetterie de cinéma haute fiabilité (Java 25, Spring Boot 4.1.1, MySQL 8.4)"))
                .addSecurityItem(new SecurityRequirement().addList(NOM_SCHEMA_SECURITE))
                .components(new Components()
                        .addSecuritySchemes(
                                NOM_SCHEMA_SECURITE,
                                new SecurityScheme()
                                        .name(NOM_SCHEMA_SECURITE)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Authentification HTTP Basic pour les rôles CLIENT, PROGRAMMATEUR et ADMIN")
                        ));
    }
}
