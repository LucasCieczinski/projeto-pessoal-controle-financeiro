package peres.lucas.apifinanceira.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginDto {

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Informe um e-mail válido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    private String senha;

    private boolean remember;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public boolean isRemember() {
        return remember;
    }

    public void setRemember(boolean remember) {
        this.remember = remember;
    }
}
