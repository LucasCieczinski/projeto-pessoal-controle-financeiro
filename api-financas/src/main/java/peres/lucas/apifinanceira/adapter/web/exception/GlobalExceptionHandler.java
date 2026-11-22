package peres.lucas.apifinanceira.adapter.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import peres.lucas.apifinanceira.domain.exception.EmailAlreadyInUseException;
import peres.lucas.apifinanceira.domain.exception.IncomeNotFoundException;
import peres.lucas.apifinanceira.domain.exception.InvalidCredentialException;
import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;
import peres.lucas.apifinanceira.domain.exception.MovementNotFoundException;
import peres.lucas.apifinanceira.domain.exception.RecurringExpenseNotFoundException;
import peres.lucas.apifinanceira.domain.exception.UserNotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MovementNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleMovementNotFound(MovementNotFoundException e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                request.getRequestURI(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IncomeNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleIncomeNotFound(IncomeNotFoundException e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                request.getRequestURI(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RecurringExpenseNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleRecurringExpenseNotFound(RecurringExpenseNotFoundException e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                request.getRequestURI(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorMessage> handleInvalidParameter(Exception e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                request.getRequestURI(),
                "Parâmetro inválido"
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleException(Exception e, HttpServletRequest request) {
        LOGGER.error("Erro interno não tratado", e);
        ErrorMessage body = new ErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                request.getRequestURI(),
                "Ocorreu um erro interno. Tente novamente mais tarde."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ErrorMessage> handleEmailAlreadyInUseException(EmailAlreadyInUseException e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                request.getRequestURI(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidCredentialException.class)
    public ResponseEntity<ErrorMessage> handleInvalidCredentialException(InvalidCredentialException e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                request.getRequestURI(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleUserNotFoundException(UserNotFoundException e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                request.getRequestURI(),
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ErrorMessage body = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                request.getRequestURI(),
                "Dados inválidos",
                fields
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleUnreadableJson(HttpMessageNotReadableException e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                request.getRequestURI(),
                "JSON inválido"
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorMessage> handleInvalidRequest(InvalidRequestException e, HttpServletRequest request) {
        ErrorMessage body = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                request.getRequestURI(),
                e.getMessage()
        );
        return ResponseEntity.badRequest().body(body);
    }
}
