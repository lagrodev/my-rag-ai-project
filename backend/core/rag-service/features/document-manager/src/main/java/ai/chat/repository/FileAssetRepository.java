package ai.chat.repository;

import ai.chat.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileAssetRepository extends JpaRepository<FileAsset, UUID>{

    Optional<FileAsset> findByHash(String hash);
}
