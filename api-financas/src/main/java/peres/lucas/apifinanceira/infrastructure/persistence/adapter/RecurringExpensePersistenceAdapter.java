package peres.lucas.apifinanceira.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.domain.model.RecurringExpense;
import peres.lucas.apifinanceira.domain.port.RecurringExpenseRepositoryPort;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.RecurringExpenseEntity;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.RecurringExpenseJpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.UserJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RecurringExpensePersistenceAdapter implements RecurringExpenseRepositoryPort {

    private final RecurringExpenseJpaRepository recurringExpenseRepository;
    private final UserJpaRepository userRepository;

    public RecurringExpensePersistenceAdapter(RecurringExpenseJpaRepository recurringExpenseRepository,
                                              UserJpaRepository userRepository) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RecurringExpense save(RecurringExpense expense) {
        RecurringExpenseEntity entity = toEntity(expense);
        RecurringExpenseEntity saved = recurringExpenseRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public List<RecurringExpense> findByUserId(UUID userId) {
        return recurringExpenseRepository.findByUser_IdOrderByDiaVencimentoAscDescricaoAsc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<RecurringExpense> findByIdAndUserId(UUID id, UUID userId) {
        return recurringExpenseRepository.findByIdAndUser_Id(id, userId)
                .map(this::toDomain);
    }

    @Override
    public void delete(RecurringExpense expense) {
        recurringExpenseRepository.deleteById(expense.getId());
    }

    private RecurringExpenseEntity toEntity(RecurringExpense domain) {
        RecurringExpenseEntity entity;
        if (domain.getId() != null) {
            entity = recurringExpenseRepository.findById(domain.getId())
                    .orElseGet(RecurringExpenseEntity::new);
            entity.setId(domain.getId());
        } else {
            entity = new RecurringExpenseEntity();
        }
        entity.setUser(userRepository.getReferenceById(domain.getUsuarioId()));
        entity.setDescricao(domain.getDescricao());
        entity.setValor(domain.getValor());
        entity.setDiaVencimento(domain.getDiaVencimento());
        entity.setAtivo(domain.isAtivo());
        return entity;
    }

    private RecurringExpense toDomain(RecurringExpenseEntity entity) {
        return new RecurringExpense(
                entity.getId(),
                entity.getUser().getId(),
                entity.getDescricao(),
                entity.getValor(),
                entity.getDiaVencimento(),
                entity.isAtivo()
        );
    }
}
