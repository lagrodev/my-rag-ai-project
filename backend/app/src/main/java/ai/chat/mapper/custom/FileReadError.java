package ai.chat.mapper.custom;

import ai.chat.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class FileReadError extends ApplicationException {
    public FileReadError(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
