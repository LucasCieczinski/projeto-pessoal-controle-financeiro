package peres.lucas.apifinanceira.adapter.web.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public class ErrorMessage {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String path;
    private final String mensagem;
    private final Map<String, String> campos;

    public ErrorMessage(String mensagem, int status) {
        this(status, "Error", "", mensagem, Collections.emptyMap());
    }

    public ErrorMessage(int status, String error, String path, String mensagem) {
        this(status, error, path, mensagem, Collections.emptyMap());
    }

    public ErrorMessage(int status, String error, String path, String mensagem, Map<String, String> campos) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.path = path;
        this.mensagem = mensagem;
        this.campos = campos;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getPath() { return path; }
    public String getMensagem() { return mensagem; }
    public Map<String, String> getCampos() { return campos; }
}
