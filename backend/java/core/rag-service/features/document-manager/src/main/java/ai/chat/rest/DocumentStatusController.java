package ai.chat.rest;

import ai.chat.backend.logging_common.aspect.Loggable;
import ai.chat.entity.DocumentStatus;
import ai.chat.entity.FileAsset;
import ai.chat.service.DocumentService;
import ai.chat.service.DocumentStatusSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Loggable(level = Loggable.LogLevel.DEBUG, slowThresholdMs = 300)
@RequestMapping("/api/documents")
public class DocumentStatusController
{
    private final DocumentStatusSseService sseService;
    private final DocumentService documentService;

    @GetMapping(value = "/{id}/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamDocumentStatus(
            @PathVariable UUID id, // это id шник документа
            @AuthenticationPrincipal Jwt jwt
    )
    {
        UUID userId = getUserIdFromJwt(jwt);

        FileAsset fileAsset = documentService.getAssetIdOrThrow(id, userId);

        SseEmitter emitter = sseService.subscribe(fileAsset.getId());

        DocumentStatus current = fileAsset.getStatus();
        if (current != null) {
            sseService.push(id, current, "state"); // sendCurrentStatusOnConnect
        }

        return new ResponseEntity<>(emitter, HttpStatus.OK);
    }

    private UUID getUserIdFromJwt(Jwt jwt)
    {
        return UUID.fromString(jwt.getClaimAsString("sub"));
    }

}


