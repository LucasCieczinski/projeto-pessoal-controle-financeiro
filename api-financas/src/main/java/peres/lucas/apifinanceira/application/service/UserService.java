package peres.lucas.apifinanceira.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;
import peres.lucas.apifinanceira.application.dto.CreateUserRequestDto;
import peres.lucas.apifinanceira.application.dto.UserResponseDto;
import peres.lucas.apifinanceira.domain.exception.EmailAlreadyInUseException;
import peres.lucas.apifinanceira.domain.exception.UserNotFoundException;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepositoryPort userRepository;

    public UserService(PasswordEncoder passwordEncoder, UserRepositoryPort userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public UserResponseDto createUser(CreateUserRequestDto request) {
        String normalizedEmail = User.normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyInUseException("Erro: Email já está sendo usado");
        }

        User novoUsuario = User.novo(
                request.nome(),
                normalizedEmail,
                passwordEncoder.encode(request.senha())
        );

        return toResponse(userRepository.save(novoUsuario));
    }

    public UserResponseDto findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    private UserResponseDto toResponse(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getDataCriacao()
        );
    }
}
