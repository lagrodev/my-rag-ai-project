package ai.chat.repository;

import ai.chat.entity.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {


    @Query(
            """
        SELECT o FROM OutboxEvent o
        WHERE o.state = :state
        AND (o.nextAttemptAt IS NULL OR o.nextAttemptAt <= :now)
        ORDER BY o.createdAt ASC
"""
    )
    Page<OutboxEvent> findPendingForRelay(@Param("now") LocalDateTime now, @Param("state") OutboxEvent.OutboxEventState state, Pageable pageable);
}
