package peres.lucas.apifinanceira.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_movimentacoes")
@Getter
@Setter
public class MovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementType tipo;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(name = "data_movimentacao", nullable = false)
    private LocalDate dataMovimentacao;

    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
