package ai.chat.repository;

import ai.chat.entity.OutboxEvent;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
//
//@Repository
//public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID>
//{
//
//
//    void updateState(UUID id, OutboxEvent.State state);
//
//    @Query("""
//        SELECT o FROM OutboxEvent o
//        WHERE o.state = 'PENDING'
//        AND o.createdAt < :threshold
//        ORDER BY o.createdAt ASC
//    """)
//    List<OutboxEvent> findStuckPendingEvents(@Param("threshold") LocalDateTime threshold);
//}
