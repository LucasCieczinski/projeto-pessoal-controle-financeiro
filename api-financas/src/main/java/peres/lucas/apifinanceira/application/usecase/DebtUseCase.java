package peres.lucas.apifinanceira.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.application.dto.DebtRequestDto;
import peres.lucas.apifinanceira.application.dto.DebtResponseDto;
import peres.lucas.apifinanceira.domain.exception.DebtNotFoundException;
import peres.lucas.apifinanceira.domain.exception.UserNotFoundException;
import peres.lucas.apifinanceira.domain.model.Debt;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.DebtRepositoryPort;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;

import java.util.List;
import java.util.UUID;

@Service
public class DebtUseCase {

    private final DebtRepositoryPort debtRepository;
    private final UserRepositoryPort userRepository;

    public DebtUseCase(DebtRepositoryPort debtRepository, UserRepositoryPort userRepository) {
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DebtResponseDto> listar(String email) {
        User user = user(email);
        return debtRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DebtResponseDto criar(DebtRequestDto dto, String email) {
        User user = user(email);
        Debt debt = Debt.nova(
                user.getId(),
                dto.credor(),
                dto.valorTotal(),
                dto.totalParcelas(),
                dto.parcelasPagas(),
                dto.status()
        );
        return toResponse(debtRepository.save(debt));
    }

    @Transactional
    public DebtResponseDto atualizar(UUID id, DebtRequestDto dto, String email) {
        Debt debt = ownedDebt(id, email);
        debt.atualizarDados(
                dto.credor(),
                dto.valorTotal(),
                dto.totalParcelas(),
                dto.parcelasPagas(),
                dto.status()
        );
        return toResponse(debtRepository.save(debt));
    }

    @Transactional
    public DebtResponseDto pagarParcela(UUID id, String email) {
        Debt debt = ownedDebt(id, email);
        debt.registrarPagamentoParcela();
        return toResponse(debtRepository.save(debt));
    }

    @Transactional
    public void excluir(UUID id, String email) {
        debtRepository.delete(ownedDebt(id, email));
    }

    private Debt ownedDebt(UUID id, String email) {
        return debtRepository.findByIdAndUserId(id, user(email).getId())
                .orElseThrow(DebtNotFoundException::new);
    }

    private User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    private DebtResponseDto toResponse(Debt debt) {
        return new DebtResponseDto(
                debt.getId(),
                debt.getCredor(),
                debt.getValorTotal(),
                debt.getTotalParcelas(),
                debt.getParcelasPagas(),
                debt.getStatus()
        );
    }
}
