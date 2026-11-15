package peres.lucas.apifinanceira.infrastructure.persistence.adapter;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;
import peres.lucas.apifinanceira.infrastructure.config.CacheConfig;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.UserEntity;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.UserJpaRepository;

import java.util.Optional;


@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository userRepository;

    public UserPersistenceAdapter(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Cacheable(value = CacheConfig.USER_BY_EMAIL_CACHE, key = "#email", unless = "#result == null")
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, key = "#user.email")
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
