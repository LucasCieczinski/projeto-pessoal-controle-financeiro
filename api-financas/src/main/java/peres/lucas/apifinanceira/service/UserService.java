package peres.lucas.apifinanceira.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import peres.lucas.apifinanceira.dto.CreateUserRequestDto;
import peres.lucas.apifinanceira.dto.UserResponseDto;
import peres.lucas.apifinanceira.entity.UserEntity;
import peres.lucas.apifinanceira.exception.EmailAlreadyInUseException;
import peres.lucas.apifinanceira.exception.UserNotFoundException;
import peres.lucas.apifinanceira.repository.UserRepository;

import java.util.Locale;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public UserResponseDto createUser(CreateUserRequestDto request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyInUseException("Erro: Email já está sendo usado");
        }

        UserEntity user = new UserEntity();
        user.setNome(request.nome().trim());
        user.setEmail(normalizedEmail);
        user.setSenha(passwordEncoder.encode(request.senha()));

        return toResponse(userRepository.save(user));
    }

    public UserResponseDto findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    private UserResponseDto toResponse(UserEntity user) {
        return new UserResponseDto(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getDataCriacao()
        );
    }
}
