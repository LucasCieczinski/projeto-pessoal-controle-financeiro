package peres.lucas.apifinanceira.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import peres.lucas.apifinanceira.security.AuthCookieService;

@Configuration
@ConditionalOnProperty(name = "app.swagger.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "cookieAuth";
        final String cookieName = AuthCookieService.ACCESS_COOKIE;

        return new OpenAPI()
                .info(new Info()
                        .title("API Financeira")
                        .version("1.0.0")
                        .description("Documentação das rotas da API Financeira com autenticação via Cookie HttpOnly."))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(cookieName)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .description("Cookie HttpOnly de acesso: " + cookieName)));
    }
}
