package ai.chat.repository;

import ai.chat.entity.Chunk;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<@NonNull Chunk,@NonNull UUID> {

}
