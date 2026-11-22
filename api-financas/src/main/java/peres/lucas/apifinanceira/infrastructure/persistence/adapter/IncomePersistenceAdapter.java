package peres.lucas.apifinanceira.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.domain.model.Income;
import peres.lucas.apifinanceira.domain.port.IncomeRepositoryPort;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.IncomeEntity;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.IncomeJpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.UserJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class IncomePersistenceAdapter implements IncomeRepositoryPort {

    private final IncomeJpaRepository incomeRepository;
    private final UserJpaRepository userRepository;

    public IncomePersistenceAdapter(IncomeJpaRepository incomeRepository, UserJpaRepository userRepository) {
        this.incomeRepository = incomeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Income save(Income income) {
        IncomeEntity entity = toEntity(income);
        IncomeEntity saved = incomeRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public List<Income> findByUserId(UUID userId) {
        return incomeRepository.findByUser_IdOrderByFonteAsc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Income> findByIdAndUserId(UUID id, UUID userId) {
        return incomeRepository.findByIdAndUser_Id(id, userId)
                .map(this::toDomain);
    }

    @Override
    public void delete(Income income) {
        incomeRepository.deleteById(income.getId());
    }

    private IncomeEntity toEntity(Income domain) {
        IncomeEntity entity;
        if (domain.getId() != null) {
            entity = incomeRepository.findById(domain.getId())
                    .orElseGet(IncomeEntity::new);
            entity.setId(domain.getId());
        } else {
            entity = new IncomeEntity();
        }
        entity.setUser(userRepository.getReferenceById(domain.getUsuarioId()));
        entity.setFonte(domain.getFonte());
        entity.setValorBase(domain.getValorBase());
        entity.setAtivo(domain.isAtivo());
        return entity;
    }

    private Income toDomain(IncomeEntity entity) {
        return new Income(
                entity.getId(),
                entity.getUser().getId(),
                entity.getFonte(),
                entity.getValorBase(),
                entity.isAtivo()
        );
    }
}
