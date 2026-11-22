package peres.lucas.apifinanceira.domain.model;

import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio pura representando uma Dívida ou Parcelamento (planejamento e controle de passivos).
 * <p>
 * Não altera o saldo realizado ({@link Movement}) automaticamente.
 */
public class Debt {

    private final UUID id;
    private final UUID usuarioId;
    private String credor;
    private BigDecimal valorTotal;
    private Integer totalParcelas;
    private int parcelasPagas;
    private DebtStatus status;

    public Debt(UUID id,
                UUID usuarioId,
                String credor,
                BigDecimal valorTotal,
                Integer totalParcelas,
                Integer parcelasPagas,
                DebtStatus status) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
        atualizarDados(credor, valorTotal, totalParcelas, parcelasPagas, status);
    }

    public static Debt nova(UUID usuarioId,
                            String credor,
                            BigDecimal valorTotal,
                            Integer totalParcelas,
                            Integer parcelasPagas,
                            DebtStatus status) {
        return new Debt(null, usuarioId, credor, valorTotal, totalParcelas, parcelasPagas, status);
    }

    public void atualizarDados(String credor,
                               BigDecimal valorTotal,
                               Integer totalParcelas,
                               Integer parcelasPagas,
                               DebtStatus status) {
        if (credor == null) {
            throw new InvalidRequestException("Credor deve ter entre 1 e 100 caracteres");
        }
        String credorLimpo = credor.trim();
        if (credorLimpo.isEmpty() || credorLimpo.length() > 100) {
            throw new InvalidRequestException("Credor deve ter entre 1 e 100 caracteres");
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Valor total deve ser maior que zero");
        }
        if (totalParcelas != null && totalParcelas <= 0) {
            throw new InvalidRequestException("Total de parcelas deve ser maior que zero");
        }

        int pagas = parcelasPagas == null ? 0 : parcelasPagas;
        if (pagas < 0) {
            throw new InvalidRequestException("Parcelas pagas não pode ser negativo");
        }
        if (totalParcelas != null && pagas > totalParcelas) {
            throw new InvalidRequestException("Parcelas pagas não pode exceder o total de parcelas");
        }

        DebtStatus statusFinal = status == null ? DebtStatus.ABERTA : status;
        if (totalParcelas != null && pagas == totalParcelas) {
            statusFinal = DebtStatus.QUITADA;
        }

        this.credor = credorLimpo;
        this.valorTotal = valorTotal;
        this.totalParcelas = totalParcelas;
        this.parcelasPagas = pagas;
        this.status = statusFinal;
    }

    public void registrarPagamentoParcela() {
        if (this.status == DebtStatus.QUITADA) {
            throw new InvalidRequestException("Dívida já está quitada");
        }
        if (this.totalParcelas == null) {
            this.parcelasPagas++;
            this.status = DebtStatus.QUITADA;
            return;
        }
        if (this.parcelasPagas >= this.totalParcelas) {
            throw new InvalidRequestException("Todas as parcelas já foram pagas");
        }
        this.parcelasPagas++;
        if (this.parcelasPagas == this.totalParcelas) {
            this.status = DebtStatus.QUITADA;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getCredor() {
        return credor;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public Integer getTotalParcelas() {
        return totalParcelas;
    }

    public int getParcelasPagas() {
        return parcelasPagas;
    }

    public DebtStatus getStatus() {
        return status;
    }
}
