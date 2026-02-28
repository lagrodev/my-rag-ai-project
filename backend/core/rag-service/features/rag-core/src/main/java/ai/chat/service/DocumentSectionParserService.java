package ai.chat.service;

import ai.chat.entity.DocumentSection;
import ai.chat.utils.Tree;

import java.util.List;
import java.util.UUID;

public interface DocumentSectionParserService {
    List<DocumentSection> parseTreeToSections(Tree parsedDocument, UUID fileAssetId);

}
