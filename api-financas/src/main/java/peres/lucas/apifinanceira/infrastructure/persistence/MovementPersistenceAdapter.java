package peres.lucas.apifinanceira.infrastructure.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.domain.model.Movement;
import peres.lucas.apifinanceira.domain.model.MovementTotals;
import peres.lucas.apifinanceira.domain.port.MovementRepositoryPort;
import peres.lucas.apifinanceira.entity.MovementEntity;
import peres.lucas.apifinanceira.repository.MovementRepository;
import peres.lucas.apifinanceira.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de infraestrutura JPA que implementa a porta {@link MovementRepositoryPort}.
 * <p>
 * Isola o uso de {@link Pageable}, {@link PageRequest} e entidades JPA na camada
 * de infraestrutura.
 */
@Component
public class MovementPersistenceAdapter implements MovementRepositoryPort {

    private final MovementRepository movementRepository;
    private final UserRepository userRepository;

    public MovementPersistenceAdapter(MovementRepository movementRepository, UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Movement save(Movement movement) {
        MovementEntity entity;
        if (movement.getId() != null) {
            entity = movementRepository.findById(movement.getId())
                    .orElseGet(MovementEntity::new);
            entity.setId(movement.getId());
        } else {
            entity = new MovementEntity();
        }
        entity.setUser(userRepository.getReferenceById(movement.getUsuarioId()));
        entity.setDescricao(movement.getDescricao());
        entity.setValor(movement.getValor());
        entity.setTipo(movement.getTipo());
        entity.setCategoria(movement.getCategoria());
        entity.setDataMovimentacao(movement.getDataMovimentacao());

        MovementEntity saved = movementRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Movement> findByIdAndUserId(UUID id, UUID userId) {
        return movementRepository.findByIdAndUser_Id(id, userId).map(this::toDomain);
    }

    @Override
    public void delete(Movement movement) {
        movementRepository.deleteById(movement.getId());
    }

    @Override
    public List<Movement> findForMonth(UUID userId, LocalDate start, LocalDate end) {
        return movementRepository.findForMonth(userId, start, end, Pageable.unpaged())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Movement> findRecentForMonth(UUID userId, LocalDate start, LocalDate end, int limit) {
        return movementRepository.findForMonth(userId, start, end, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<MovementTotals> summarize(UUID userId, LocalDate start, LocalDate end) {
        return movementRepository.summarize(userId, start, end)
                .stream()
                .map(t -> new MovementTotals(
                        t.getTipo(),
                        t.getTotal(),
                        t.getMonthTotal(),
                        t.getMonthCount(),
                        t.getHistoryCount()
                ))
                .toList();
    }

    private Movement toDomain(MovementEntity entity) {
        return new Movement(
                entity.getId(),
                entity.getUser().getId(),
                entity.getDescricao(),
                entity.getValor(),
                entity.getTipo(),
                entity.getCategoria(),
                entity.getDataMovimentacao(),
                entity.getDataCadastro()
        );
    }
}
