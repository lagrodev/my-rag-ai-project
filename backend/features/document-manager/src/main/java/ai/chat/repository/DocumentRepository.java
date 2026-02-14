package ai.chat.repository;

import ai.chat.entity.Document;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<@NonNull Document, @NonNull UUID> {

    Page<@NonNull Document> findByFilenameContainingIgnoreCase(String filename, Pageable pageable);

    Page<@NonNull Document> findByFilenameContainingIgnoreCaseAndUploadedBy(String filename, UUID uploadedBy, Pageable pageable);

    Page<@NonNull Document>  findAllByUploadedBy(UUID uploadedBy, Pageable pageable);
}
