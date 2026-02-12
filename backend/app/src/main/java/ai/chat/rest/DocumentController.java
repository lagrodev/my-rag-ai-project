package ai.chat.rest;

import ai.chat.rest.dto.DocumentDto;
import ai.chat.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import java.util.UUID;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<@NonNull Page<@NonNull DocumentDto>> getDocuments(
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(documentService.findAll(userId, pageable));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<@NonNull DocumentDto> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ){
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(documentService.uploadDocument(file, userId));
    }



    @GetMapping("/{id}")
    public ResponseEntity<@NonNull DocumentDto> getDocumentMetadata(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(documentService.getDocumentMetadata(id, userId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<@NonNull Resource> downloadDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        DocumentDto metadata = documentService.getDocumentMetadata(id, userId); // Получаем имя файла для заголовка
        Resource resource = documentService.downloadDocument(id, userId);


        String encodedFilename = java.net.URLEncoder.encode(metadata.filename(), java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType())) // Лучше реальный тип, чем octet-stream
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @GetMapping("/search")
    public ResponseEntity<@NonNull Page<@NonNull DocumentDto>> searchByFilename(
            @RequestParam String filename,
            Pageable pageable,
                @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(documentService.searchByFilename(filename, userId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        documentService.deleteDocument(id, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserIdFromJwt(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("sub"));
    }

}
