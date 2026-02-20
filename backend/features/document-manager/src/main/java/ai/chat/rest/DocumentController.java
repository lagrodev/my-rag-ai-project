package ai.chat.rest;

import ai.chat.rest.dto.ConfirmUploadRequest;
import ai.chat.rest.dto.DocumentDto;
import ai.chat.rest.dto.InitUploadRequest;
import ai.chat.rest.dto.InitUploadResponse;
import ai.chat.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
//        return BaseDto.of(DocumentDto);
        // static BaseDto<T>.of -> responseentity<BaseDTO> = BaseDTO<T> {status, message, error, errorMessage, content: T}
        return ResponseEntity.ok(documentService.findAll(userId, pageable));
    }


    @PostMapping("/init-upload")
    public ResponseEntity<InitUploadResponse> uploadDocument(
            @RequestBody @Valid InitUploadRequest request,
            @AuthenticationPrincipal Jwt jwt
    ){
        UUID userId = getUserIdFromJwt(jwt);

        return ResponseEntity.ok(documentService.initDocumentUpload(request, userId));
    }



    @PostMapping("/confirm-upload")
    public ResponseEntity<DocumentDto> confirmDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ConfirmUploadRequest  request
    ){
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(documentService.confirmDocumentUpload(request, userId));
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
    public ResponseEntity<@NonNull String> downloadDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        String url = documentService.getDocumentDownloadUrl(id, userId);
        return ResponseEntity.ok()
                .body(url);
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
