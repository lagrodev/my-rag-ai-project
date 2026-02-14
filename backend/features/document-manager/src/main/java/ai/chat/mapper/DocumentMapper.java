package ai.chat.mapper;

import ai.chat.entity.Document;
import ai.chat.rest.dto.DocumentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    @Mapping(target = "isIndexed", constant = "false") // Default value for new documents
    DocumentDto toDto(Document document);
}
