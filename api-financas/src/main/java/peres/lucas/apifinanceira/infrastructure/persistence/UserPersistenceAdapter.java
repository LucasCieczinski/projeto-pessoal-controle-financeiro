package peres.lucas.apifinanceira.infrastructure.persistence;

import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;
import peres.lucas.apifinanceira.entity.UserEntity;
import peres.lucas.apifinanceira.repository.UserRepository;

import java.util.Optional;

/**
 * Adaptador de infraestrutura JPA que implementa a porta {@link UserRepositoryPort}.
 * <p>
 * Converte transparentemente entre o modelo de domínio puro {@link User} e a
 * entidade de banco de dados {@link UserEntity}.
 */
@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserRepository userRepository;

    public UserPersistenceAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = userRepository.save(entity);
        return toDomain(saved);
    }

    private User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getNome(),
                entity.getEmail(),
                entity.getSenha(),
                entity.getDataCriacao()
        );
    }

    private UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        entity.setId(domain.getId());
        entity.setNome(domain.getNome());
        entity.setEmail(domain.getEmail());
        entity.setSenha(domain.getSenhaHash());
        entity.setDataCriacao(domain.getDataCriacao());
        return entity;
    }
}
