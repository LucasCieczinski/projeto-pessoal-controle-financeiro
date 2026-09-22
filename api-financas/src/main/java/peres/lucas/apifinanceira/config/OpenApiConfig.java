package peres.lucas.apifinanceira.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "cookieAuth";
        final String cookieName = "ACCESS_COOKIE";

        return new OpenAPI()
                .info(new Info()
                        .title("API Financeira")
                        .version("1.0.0")
                        .description("Documentação das rotas da API Financeira com autenticação via Cookie."))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(cookieName)            // Nome do cookie lido no filtro
                                        .type(SecurityScheme.Type.APIKEY) // Define como API Key
                                        .in(SecurityScheme.In.COOKIE)));  // Indica que o token viaja via Cookie
    }
}
