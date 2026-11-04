package peres.lucas.apifinanceira;

import org.junit.jupiter.api.Test;
import peres.lucas.apifinanceira.application.dto.CreateUserRequestDto;
import peres.lucas.apifinanceira.application.dto.UserResponseDto;
import peres.lucas.apifinanceira.infrastructure.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import peres.lucas.apifinanceira.adapter.web.controller.UserController;
import peres.lucas.apifinanceira.domain.exception.EmailAlreadyInUseException;
import peres.lucas.apifinanceira.infrastructure.security.SecurityConfig;
import peres.lucas.apifinanceira.application.usecase.UserService;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.AuthSessionRepository;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtUtil.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthSessionRepository sessionRepository;

    private CreateUserRequestDto buildUser() {
        return new CreateUserRequestDto("Lucas Peres", "lucas@teste.com", "senha123");
    }

    @Test
    void deveRetornar201SemSenha_QuandoUsuarioValido() throws Exception {
        CreateUserRequestDto user = buildUser();
        UserResponseDto response = new UserResponseDto(
                UUID.randomUUID(),
                user.nome(),
                user.email(),
                LocalDateTime.now()
        );
        doReturn(response).when(userService).createUser(any(CreateUserRequestDto.class));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("lucas@teste.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void deveRetornar400_QuandoSenhaEstaVazia() throws Exception {
        CreateUserRequestDto user = new CreateUserRequestDto("Lucas Peres", "lucas@teste.com", "");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400_QuandoEmailEstaNulo() throws Exception {
        CreateUserRequestDto user = new CreateUserRequestDto("Lucas Peres", null, "senha123");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar409_QuandoEmailJaEstaEmUso() throws Exception {
        CreateUserRequestDto user = buildUser();
        doThrow(new EmailAlreadyInUseException("Erro: Email já está sendo usado"))
                .when(userService).createUser(any(CreateUserRequestDto.class));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(user)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Erro: Email já está sendo usado"));
    }
}
