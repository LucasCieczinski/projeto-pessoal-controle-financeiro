package peres.lucas.apifinanceira.domain.model;

import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio pura representando um Gasto Recorrente mensal (compromisso financeiro fixo).
 * <p>
 * Não altera o saldo realizado ({@link Movement}) automaticamente.
 */
public class RecurringExpense {

    private final UUID id;
    private final UUID usuarioId;
    private String descricao;
    private BigDecimal valor;
    private int diaVencimento;
    private boolean ativo;

    public RecurringExpense(UUID id, UUID usuarioId, String descricao, BigDecimal valor, int diaVencimento, boolean ativo) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
        atualizarDados(descricao, valor, diaVencimento, ativo);
    }

    public static RecurringExpense nova(UUID usuarioId, String descricao, BigDecimal valor, int diaVencimento, boolean ativo) {
        return new RecurringExpense(null, usuarioId, descricao, valor, diaVencimento, ativo);
    }

    public void atualizarDados(String descricao, BigDecimal valor, int diaVencimento, boolean ativo) {
        if (descricao == null) {
            throw new InvalidRequestException("Descrição é obrigatória");
        }
        String descricaoLimpa = descricao.trim();
        if (descricaoLimpa.isEmpty() || descricaoLimpa.length() > 150) {
            throw new InvalidRequestException("Descrição deve ter entre 1 e 150 caracteres");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Valor deve ser maior que zero");
        }
        if (diaVencimento < 1 || diaVencimento > 31) {
            throw new InvalidRequestException("Dia de vencimento deve ser entre 1 e 31");
        }

        this.descricao = descricaoLimpa;
        this.valor = valor;
        this.diaVencimento = diaVencimento;
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public int getDiaVencimento() {
        return diaVencimento;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
