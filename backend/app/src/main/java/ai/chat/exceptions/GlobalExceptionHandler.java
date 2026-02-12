package ai.chat.exceptions;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j // Logger для логирования ошибок
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<AppError> handleApplicationException(ApplicationException ex) {
        AppError body = new AppError(
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex.getStatus().value()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        AppError error = new AppError("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(error);
    }



    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<AppError> handleValidationException(ValidationException ex) {
        AppError error = new AppError("ValidationException", ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<AppError> handleLockedException(LockedException ex) {
        AppError error = new AppError("ACCOUNT_LOCKED", ex.getMessage(), HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppError> handleUnexpectedException(Exception ex) {
        log.error("Unexpected internal server error occurred", ex);
        AppError error = new AppError(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AppError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value for parameter '%s': '%s'", ex.getName(), ex.getValue());
        AppError error = new AppError("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(error);
    }


}
