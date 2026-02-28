package ai.chat.repository;

import ai.chat.entity.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QueryLogRepository extends JpaRepository<QueryLog, UUID> {

    List<QueryLog> findByFileAssetIdOrderByQueriedAtDesc(UUID fileAssetId);

    List<QueryLog> findByUserIdOrderByQueriedAtDesc(UUID userId);
}
