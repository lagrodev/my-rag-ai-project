package ai.chat.service.impl;

import ai.chat.entity.DocumentSection;
import ai.chat.repository.DocumentSectionRepository;
import ai.chat.service.DocumentSectionParserService;
import ai.chat.utils.Tree;
import ai.chat.utils.TreeNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentSectionParserServiceImpl implements DocumentSectionParserService
{
    private final DocumentSectionRepository documentSectionRepository;

    @Override
    public List<DocumentSection> parseTreeToSections(Tree parsedDocument, UUID fileAssetId)
    {
        List<DocumentSection> sections = new ArrayList<>();
        if (parsedDocument != null && parsedDocument.getRoot() != null)
        {
            dfs(parsedDocument.getRoot(), "", 1, sections, null, fileAssetId); // тут передаем: наш текущий код дерева - начинается с 1, наши посещенные секции
            // т.к. тут мы сслыку передаем, то не обязательно чет сохранять, ссылку на родича, путь, уровень текущий
        }

        return documentSectionRepository.saveAll(sections);
    }


    private void dfs(
            TreeNode node,
            String parentPath,
            int level,
            List<DocumentSection> documentSections,
            DocumentSection parent,
            UUID fileAssetId
    )
    {
        String currentPath = parentPath.isEmpty() ? String.valueOf(level) : parentPath + "." + level;

        DocumentSection documentSection = buildDocumentSection(node, currentPath, level, parent, fileAssetId);

        if (node.getHeading() != null)
        {
            documentSection.setHeadingTitle(node.getHeading().getTitle());
            if (node.getHeading().getDepth() != null)
            {
                documentSection.setHeadingDepth(node.getHeading().getDepth().intValue());
            }
        }

        documentSections.add(documentSection);


        if (node.getChildren() != null && !node.getChildren().isEmpty())
        {
            int counter = 1;
            for (TreeNode child : node.getChildren())
            {

                dfs(
                        child,
                        currentPath,
                        counter++,
                        documentSections,
                        documentSection,
                        fileAssetId
                );
            }
        }
    }

    private DocumentSection buildDocumentSection(TreeNode node, String currentPath, int level, DocumentSection parent, UUID fileAssetId)
    {
        return DocumentSection.builder()
                .path(currentPath)
                .content(node.getContent())
                .fileAssetId(fileAssetId)
                .sequenceNumber(level)
                .parentSection(parent)
                .build();
    }

    // todo - да
    private void iterDfs(
            TreeNode node,
            String parentPath,
            DocumentSection parent,
            int sequenceNumber,
            List<DocumentSection> documentSections,
            UUID fileAssetId
    )
    {

    }


}
