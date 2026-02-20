package ai.chat.exceptions;

import lombok.Data;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Data
public class AppError {
    private int status;
    private String error;
    private String message;
    private Instant timestamp;
    private Map<String, String> validationErrors;

    public AppError(String error, String message, int status){
        this.timestamp = Instant.now();
        this.status = status;
        this.message = message;
        this.error = error;
    }


    public AppError(String error, String message, int status, Map<String, String> validationErrors) {
        this(error, message, status); // Вызываем базовый конструктор
        this.validationErrors = validationErrors;
    }
}
