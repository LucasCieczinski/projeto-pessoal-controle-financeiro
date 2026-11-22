package peres.lucas.apifinanceira.infrastructure.config;

/**
 * Constantes centralizadas para o versionamento das rotas da API REST.
 * <p>
 * Evita espalhar "magic strings" de caminhos entre Controllers, SecurityConfig,
 * RateLimitFilter e AuthCookieService, garantindo consistência de contrato.
 */
public final class ApiPaths {

    public static final String V1 = "/api/v1";

    public static final String AUTH = V1 + "/auth";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_REFRESH = AUTH + "/refresh";
    public static final String AUTH_LOGOUT = AUTH + "/logout";
    public static final String AUTH_CSRF = AUTH + "/csrf";

    public static final String USERS = V1 + "/users";
    public static final String MOVEMENTS = V1 + "/movimentacoes";
    public static final String INCOMES = V1 + "/rendas";
    public static final String RECURRING_EXPENSES = V1 + "/gastos-recorrentes";
    public static final String DEBTS = V1 + "/dividas";

    private ApiPaths() {
        // Classe utilitária de constantes
    }
}
