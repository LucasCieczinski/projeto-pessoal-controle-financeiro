package peres.lucas.apifinanceira.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tb_cartoes_credito")
@Getter
@Setter
public class CreditCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity user;

    @Column(name = "nome_cartao", nullable = false, length = 50)
    private String nomeCartao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal limite;

    @Column(name = "dia_fechamento", nullable = false)
    private int diaFechamento;

    @Column(name = "dia_vencimento", nullable = false)
    private int diaVencimento;
}
