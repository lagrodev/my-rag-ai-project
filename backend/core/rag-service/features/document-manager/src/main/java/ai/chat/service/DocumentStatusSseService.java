package ai.chat.service;

import ai.chat.entity.DocumentStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface DocumentStatusSseService {
    SseEmitter subscribe(UUID fileAssetId);

    void push(UUID fileAssetId, DocumentStatus documentStatus, String message);

    void push(UUID fileAssetId, String documentStatus, String message);
}
