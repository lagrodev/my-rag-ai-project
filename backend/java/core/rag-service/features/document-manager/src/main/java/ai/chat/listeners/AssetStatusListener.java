package ai.chat.listeners;

import ai.chat.entity.DocumentStatus;
import ai.chat.entity.FileAsset;
import ai.chat.events.DocumentStatusEvent;
import ai.chat.repository.FileAssetRepository;
import ai.chat.service.DocumentStatusSseService;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetStatusListener {
    private final FileAssetRepository fileAssetRepository;
    private final DocumentStatusSseService sseService;
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void listen(DocumentStatusEvent event)
    {
        log.debug("Обновление статуса asset={} на {}", event.fileAssetId(), event.status());
        FileAsset asset = fileAssetRepository.findById(event.fileAssetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + event.fileAssetId()));

        asset.setStatus(event.status());
        asset.setStateMessage(event.message());

        event.status().applySpecificFields(asset, event.message());

        sseService.push(
                event.fileAssetId(), event.status(), event.message()
        );
    }
}
