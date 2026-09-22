package peres.lucas.apifinanceira.exception;

import java.util.Collections;
import java.util.Map;

public class ErrorMessage {

    private final String mensagem;
    private final int status;
    private final Map<String, String> campos;

    public ErrorMessage(String mensagem, int status) {
        this(mensagem, status, Collections.emptyMap());
    }

    public ErrorMessage(String mensagem, int status, Map<String, String> campos) {
        this.mensagem = mensagem;
        this.status = status;
        this.campos = campos;
    }

    public String getMensagem() { return mensagem; }
    public int getStatus() { return status; }
    public Map<String, String> getCampos() { return campos; }
}
