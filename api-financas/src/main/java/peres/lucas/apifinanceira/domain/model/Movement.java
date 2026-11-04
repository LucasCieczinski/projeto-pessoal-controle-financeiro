package peres.lucas.apifinanceira.domain.model;

import peres.lucas.apifinanceira.entity.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio pura representando uma Movimentação Financeira.
 * <p>
 * Encapsula as regras de higienização (trim de descrição e categoria) no próprio
 * núcleo de negócio, sem acoplamento com anotações de banco de dados (JPA).
 */
public class Movement {

    private final UUID id;
    private final UUID usuarioId;
    private String descricao;
    private BigDecimal valor;
    private MovementType tipo;
    private String categoria;
    private LocalDate dataMovimentacao;
    private final LocalDateTime dataCadastro;

    public Movement(UUID id, UUID usuarioId, String descricao, BigDecimal valor,
                    MovementType tipo, String categoria, LocalDate dataMovimentacao,
                    LocalDateTime dataCadastro) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
        this.dataCadastro = dataCadastro;
        atualizarDados(descricao, valor, tipo, categoria, dataMovimentacao);
    }

    public static Movement nova(UUID usuarioId, String descricao, BigDecimal valor,
                                MovementType tipo, String categoria, LocalDate dataMovimentacao) {
        return new Movement(null, usuarioId, descricao, valor, tipo, categoria, dataMovimentacao, null);
    }

    public void atualizarDados(String descricao, BigDecimal valor, MovementType tipo,
                               String categoria, LocalDate dataMovimentacao) {
        this.descricao = Objects.requireNonNull(descricao, "Descrição é obrigatória").trim();
        this.valor = Objects.requireNonNull(valor, "Valor é obrigatório");
        this.tipo = Objects.requireNonNull(tipo, "Tipo é obrigatório");
        this.categoria = Objects.requireNonNull(categoria, "Categoria é obrigatória").trim();
        this.dataMovimentacao = Objects.requireNonNull(dataMovimentacao, "Data da movimentação é obrigatória");
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

    public MovementType getTipo() {
        return tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public LocalDate getDataMovimentacao() {
        return dataMovimentacao;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }
}
