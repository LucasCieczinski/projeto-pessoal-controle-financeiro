package peres.lucas.apifinanceira.domain.model;

import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio pura representando uma Fonte de Renda prevista (planejamento financeiro).
 * <p>
 * Não altera o saldo realizado ({@link Movement}) automaticamente.
 */
public class Income {

    private final UUID id;
    private final UUID usuarioId;
    private String fonte;
    private BigDecimal valorBase;
    private boolean ativo;

    public Income(UUID id, UUID usuarioId, String fonte, BigDecimal valorBase, boolean ativo) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
        atualizarDados(fonte, valorBase, ativo);
    }

    public static Income nova(UUID usuarioId, String fonte, BigDecimal valorBase, boolean ativo) {
        return new Income(null, usuarioId, fonte, valorBase, ativo);
    }

    public void atualizarDados(String fonte, BigDecimal valorBase, boolean ativo) {
        if (fonte == null) {
            throw new InvalidRequestException("Fonte de renda é obrigatória");
        }
        String fonteLimpa = fonte.trim();
        if (fonteLimpa.isEmpty() || fonteLimpa.length() > 100) {
            throw new InvalidRequestException("Fonte de renda deve ter entre 1 e 100 caracteres");
        }
        if (valorBase == null || valorBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Valor base deve ser maior que zero");
        }

        this.fonte = fonteLimpa;
        this.valorBase = valorBase;
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getFonte() {
        return fonte;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
