package ai.chat.repository;

import ai.chat.entity.DocumentStatus;
import ai.chat.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileAssetRepository extends JpaRepository<FileAsset, UUID>{

    Optional<FileAsset> findByHash(String hash);

}
