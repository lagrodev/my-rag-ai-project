package ai.chat.exceptions;

import lombok.Data;

import java.util.Date;

@Data
public class AppError {
    private int status;
    private String error;
    private String message;
    private Date timestamp;

    public AppError(String error, String message, int status){
        this.timestamp = new Date();
        this.status = status;
        this.message = message;
        this.error = error;
    }
}
