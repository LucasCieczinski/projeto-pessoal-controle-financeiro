package peres.lucas.apifinanceira.domain.model;

import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio pura representando um Cartão de Crédito (planejamento e controle de limites).
 * <p>
 * Não altera o saldo realizado ({@link Movement}) automaticamente.
 */
public class CreditCard {

    private final UUID id;
    private final UUID usuarioId;
    private String nomeCartao;
    private BigDecimal limite;
    private int diaFechamento;
    private int diaVencimento;

    public CreditCard(UUID id,
                      UUID usuarioId,
                      String nomeCartao,
                      BigDecimal limite,
                      int diaFechamento,
                      int diaVencimento) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
        atualizarDados(nomeCartao, limite, diaFechamento, diaVencimento);
    }

    public static CreditCard nova(UUID usuarioId,
                                  String nomeCartao,
                                  BigDecimal limite,
                                  int diaFechamento,
                                  int diaVencimento) {
        return new CreditCard(null, usuarioId, nomeCartao, limite, diaFechamento, diaVencimento);
    }

    public void atualizarDados(String nomeCartao,
                               BigDecimal limite,
                               int diaFechamento,
                               int diaVencimento) {
        if (nomeCartao == null) {
            throw new InvalidRequestException("Nome do cartão é obrigatório");
        }
        String nomeLimpo = nomeCartao.trim();
        if (nomeLimpo.isEmpty() || nomeLimpo.length() > 50) {
            throw new InvalidRequestException("Nome do cartão deve ter entre 1 e 50 caracteres");
        }
        if (limite == null || limite.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("Limite não pode ser negativo");
        }
        if (diaFechamento < 1 || diaFechamento > 31) {
            throw new InvalidRequestException("Dia de fechamento deve ser entre 1 e 31");
        }
        if (diaVencimento < 1 || diaVencimento > 31) {
            throw new InvalidRequestException("Dia de vencimento deve ser entre 1 e 31");
        }

        this.nomeCartao = nomeLimpo;
        this.limite = limite;
        this.diaFechamento = diaFechamento;
        this.diaVencimento = diaVencimento;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getNomeCartao() {
        return nomeCartao;
    }

    public BigDecimal getLimite() {
        return limite;
    }

    public int getDiaFechamento() {
        return diaFechamento;
    }

    public int getDiaVencimento() {
        return diaVencimento;
    }
}
