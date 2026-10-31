package com.epfjava.horsecopilote.common.presentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation served by springdoc: Swagger UI on /swagger-ui.html, OpenAPI JSON on /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    private static final String SESSION_COOKIE = "sessionCookie";

    private static final String DESCRIPTION = """
            API REST du CRM HorseCo-Pilote (centre équestre) : chevaux, créneaux, leçons, membres et coachs.

            **Authentification par session.** Appeler d'abord `POST /login` avec
            `{"email": "admin@horseco.test", "password": "password"}` (comptes de test du profil `dev`) :
            le serveur renvoie un cookie `JSESSIONID` que le navigateur réutilise pour les appels suivants
            depuis cette page. `POST /logout` ferme la session.

            Les droits dépendent du rôle (`MEMBRE`, `COACH`, `ADMIN`) : un appel non autorisé renvoie 403,
            sans session 401. Les erreurs sont des `ProblemDetail` : 400 validation (détail par champ dans
            `errors`), 404 ressource introuvable, 409 règle métier non respectée.
            """;

    @Bean
    public OpenAPI horseCoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HorseCo-Pilote API")
                        .version("v1")
                        .description(DESCRIPTION))
                .components(new Components().addSecuritySchemes(SESSION_COOKIE, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")
                        .description("Cookie de session obtenu avec POST /login")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE));
    }
}
