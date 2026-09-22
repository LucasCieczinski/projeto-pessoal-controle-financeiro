package peres.lucas.apifinanceira.entity;


import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "tb_usuarios")
@Getter @Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String senha;

    @CreationTimestamp
    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

}
