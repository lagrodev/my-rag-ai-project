package ai.chat.mapper.custom;


public class DocumentNotFoundException extends NotFoundException {

    public DocumentNotFoundException(String message) {
        super(message);
    }

    public DocumentNotFoundException(String message, String... args) {
        super(message, args);
    }
}
