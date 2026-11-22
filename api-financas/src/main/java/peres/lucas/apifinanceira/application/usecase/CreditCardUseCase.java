package peres.lucas.apifinanceira.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.application.dto.CreditCardRequestDto;
import peres.lucas.apifinanceira.application.dto.CreditCardResponseDto;
import peres.lucas.apifinanceira.domain.exception.CreditCardNotFoundException;
import peres.lucas.apifinanceira.domain.exception.UserNotFoundException;
import peres.lucas.apifinanceira.domain.model.CreditCard;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.CreditCardRepositoryPort;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;

import java.util.List;
import java.util.UUID;

@Service
public class CreditCardUseCase {

    private final CreditCardRepositoryPort creditCardRepository;
    private final UserRepositoryPort userRepository;

    public CreditCardUseCase(CreditCardRepositoryPort creditCardRepository,
                             UserRepositoryPort userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CreditCardResponseDto> listar(String email) {
        User user = user(email);
        return creditCardRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CreditCardResponseDto criar(CreditCardRequestDto dto, String email) {
        User user = user(email);
        CreditCard creditCard = CreditCard.nova(
                user.getId(),
                dto.nomeCartao(),
                dto.limite(),
                dto.diaFechamento(),
                dto.diaVencimento()
        );
        return toResponse(creditCardRepository.save(creditCard));
    }

    @Transactional
    public CreditCardResponseDto atualizar(UUID id, CreditCardRequestDto dto, String email) {
        CreditCard creditCard = ownedCreditCard(id, email);
        creditCard.atualizarDados(
                dto.nomeCartao(),
                dto.limite(),
                dto.diaFechamento(),
                dto.diaVencimento()
        );
        return toResponse(creditCardRepository.save(creditCard));
    }

    @Transactional
    public void excluir(UUID id, String email) {
        creditCardRepository.delete(ownedCreditCard(id, email));
    }

    private CreditCard ownedCreditCard(UUID id, String email) {
        return creditCardRepository.findByIdAndUserId(id, user(email).getId())
                .orElseThrow(CreditCardNotFoundException::new);
    }

    private User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    private CreditCardResponseDto toResponse(CreditCard creditCard) {
        return new CreditCardResponseDto(
                creditCard.getId(),
                creditCard.getNomeCartao(),
                creditCard.getLimite(),
                creditCard.getDiaFechamento(),
                creditCard.getDiaVencimento()
        );
    }
}
