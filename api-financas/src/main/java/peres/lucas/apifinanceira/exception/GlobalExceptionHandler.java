package peres.lucas.apifinanceira.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MovementNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleMovementNotFound(MovementNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(e.getMessage(), 404));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorMessage> handleInvalidParameter(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorMessage("Parâmetro inválido", 400));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleException(Exception e) {
        LOGGER.error("Erro interno não tratado", e);
        ErrorMessage erro = new ErrorMessage("Ocorreu um erro interno. Tente novamente mais tarde.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ErrorMessage> handleEmailAlreadyInUseException(EmailAlreadyInUseException e) {
        ErrorMessage error = new ErrorMessage(e.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidCredentialException.class)
    public ResponseEntity<ErrorMessage> handleInvalidCredentialException(InvalidCredentialException e) {
        ErrorMessage errorMessage = new ErrorMessage(e.getMessage(), HttpStatus.UNAUTHORIZED.value());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleUserNotFoundException(UserNotFoundException e) {
        ErrorMessage errorMessage = new ErrorMessage(e.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ErrorMessage errorMessage = new ErrorMessage(
                "Dados inválidos",
                HttpStatus.BAD_REQUEST.value(),
                fields
        );
        return ResponseEntity.badRequest().body(errorMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleUnreadableJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorMessage("JSON inválido", HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorMessage> handleInvalidRequest(InvalidRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }
}
