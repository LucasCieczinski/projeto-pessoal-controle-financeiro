package peres.lucas.apifinanceira.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.dto.CreateMovementRequestDto;
import peres.lucas.apifinanceira.dto.MovementResponseDto;
import peres.lucas.apifinanceira.entity.MovementEntity;
import peres.lucas.apifinanceira.entity.UserEntity;
import peres.lucas.apifinanceira.exception.UserNotFoundException;
import peres.lucas.apifinanceira.repository.MovementRepository;
import peres.lucas.apifinanceira.repository.UserRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import peres.lucas.apifinanceira.dto.MovementSummaryDto;
import peres.lucas.apifinanceira.entity.MovementType;
import peres.lucas.apifinanceira.exception.MovementNotFoundException;

@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final UserRepository userRepository;

    public MovementService(MovementRepository movementRepository, UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MovementResponseDto> findForMonth(String email, YearMonth month) {
        UserEntity user = user(email);
        return movementRepository.findForMonth(user.getId(), month.atDay(1), month.plusMonths(1).atDay(1), Pageable.unpaged())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MovementResponseDto create(String email, CreateMovementRequestDto request) {
        MovementEntity movement = new MovementEntity();
        movement.setUser(user(email));
        apply(movement, request);
        return toResponse(movementRepository.saveAndFlush(movement));
    }

    @Transactional
    public MovementResponseDto update(String email, UUID id, CreateMovementRequestDto request) {
        MovementEntity movement = ownedMovement(email, id);
        apply(movement, request);
        return toResponse(movementRepository.saveAndFlush(movement));
    }

    @Transactional
    public void delete(String email, UUID id) {
        movementRepository.delete(ownedMovement(email, id));
    }

    @Transactional(readOnly = true)
    public MovementSummaryDto summary(String email, YearMonth month) {
        UUID userId = user(email).getId();
        var start = month.atDay(1);
        var end = month.plusMonths(1).atDay(1);
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO, balance = BigDecimal.ZERO;
        long count = 0, historyCount = 0;
        for (var totals : movementRepository.summarize(userId, start, end)) {
            boolean isIncome = totals.getTipo() == MovementType.ENTRADA;
            if (isIncome) income = totals.getMonthTotal();
            else expense = totals.getMonthTotal();
            balance = balance.add(isIncome ? totals.getTotal() : totals.getTotal().negate());
            count += totals.getMonthCount();
            historyCount += totals.getHistoryCount();
        }
        BigDecimal result = income.subtract(expense);
        var recent = movementRepository.findForMonth(userId, start, end, PageRequest.of(0, 5))
                .stream().map(this::toResponse).toList();
        return new MovementSummaryDto(month.toString(), income, expense, result, balance.subtract(result),
                balance, count, historyCount, recent);
    }

    private MovementEntity ownedMovement(String email, UUID id) {
        return movementRepository.findByIdAndUser_Id(id, user(email).getId())
                .orElseThrow(MovementNotFoundException::new);
    }

    private void apply(MovementEntity movement, CreateMovementRequestDto request) {
        movement.setDescricao(request.descricao().trim());
        movement.setValor(request.valor());
        movement.setTipo(request.tipo());
        movement.setCategoria(request.categoria().trim());
        movement.setDataMovimentacao(request.dataMovimentacao());
    }

    private UserEntity user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    private MovementResponseDto toResponse(MovementEntity movement) {
        return new MovementResponseDto(
                movement.getId(),
                movement.getDescricao(),
                movement.getValor(),
                movement.getTipo(),
                movement.getCategoria(),
                movement.getDataMovimentacao(),
                movement.getDataCadastro()
        );
    }
}
