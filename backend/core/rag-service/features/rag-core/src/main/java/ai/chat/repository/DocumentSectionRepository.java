package ai.chat.repository;

import ai.chat.entity.DocumentSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentSectionRepository extends JpaRepository<DocumentSection, UUID>
{
}
