package peres.lucas.apifinanceira.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.domain.model.Debt;
import peres.lucas.apifinanceira.domain.port.DebtRepositoryPort;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.DebtEntity;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.DebtJpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.UserJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DebtPersistenceAdapter implements DebtRepositoryPort {

    private final DebtJpaRepository debtRepository;
    private final UserJpaRepository userRepository;

    public DebtPersistenceAdapter(DebtJpaRepository debtRepository, UserJpaRepository userRepository) {
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Debt save(Debt debt) {
        DebtEntity entity = toEntity(debt);
        DebtEntity saved = debtRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public List<Debt> findByUserId(UUID userId) {
        return debtRepository.findByUser_IdOrderByCredorAsc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Debt> findByIdAndUserId(UUID id, UUID userId) {
        return debtRepository.findByIdAndUser_Id(id, userId)
                .map(this::toDomain);
    }

    @Override
    public void delete(Debt debt) {
        debtRepository.deleteById(debt.getId());
    }

    private DebtEntity toEntity(Debt domain) {
        DebtEntity entity;
        if (domain.getId() != null) {
            entity = debtRepository.findById(domain.getId())
                    .orElseGet(DebtEntity::new);
            entity.setId(domain.getId());
        } else {
            entity = new DebtEntity();
        }
        entity.setUser(userRepository.getReferenceById(domain.getUsuarioId()));
        entity.setCredor(domain.getCredor());
        entity.setValorTotal(domain.getValorTotal());
        entity.setTotalParcelas(domain.getTotalParcelas());
        entity.setParcelasPagas(domain.getParcelasPagas());
        entity.setStatus(domain.getStatus());
        return entity;
    }

    private Debt toDomain(DebtEntity entity) {
        return new Debt(
                entity.getId(),
                entity.getUser().getId(),
                entity.getCredor(),
                entity.getValorTotal(),
                entity.getTotalParcelas(),
                entity.getParcelasPagas(),
                entity.getStatus()
        );
    }
}
