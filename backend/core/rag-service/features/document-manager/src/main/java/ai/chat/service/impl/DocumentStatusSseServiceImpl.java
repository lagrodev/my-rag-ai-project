package ai.chat.service.impl;

import ai.chat.entity.DocumentStatus;
import ai.chat.events.DocumentStatusEvent;
import ai.chat.service.DocumentStatusSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStatusSseServiceImpl implements DocumentStatusSseService
{
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    // чтобы при добавлении или удалении элемента создается копия всего массива, модифицируется, а затем ссылка на старый массив заменяется новой.
    @Override
    public SseEmitter subscribe(UUID fileAssetId)
    {
        SseEmitter emitter = new SseEmitter(5*60*1000L); // при разрыве соединения, на фронте должен быть механизм повторного соединения
        emitters.computeIfAbsent(
                fileAssetId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> removeEmitter(fileAssetId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    @Override
    public void push(UUID fileAssetId, DocumentStatus documentStatus, String message)
    {
        List<SseEmitter> list = emitters.get(fileAssetId);

        if (list == null || list.isEmpty()) {
            return;
        }
        push(
                fileAssetId, documentStatus, message, list
        );
    }

    private void push(
            UUID fileAssetId, DocumentStatus documentStatus, String message, List<SseEmitter> list
    ){
        DocumentStatusEvent event = new DocumentStatusEvent(
                fileAssetId, documentStatus, message, Instant.now()
        );

        // Бежим по списку подписчиков
        list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("document-status")
                        .data(event, MediaType.APPLICATION_JSON));

                if (documentStatus == DocumentStatus.READY || documentStatus == DocumentStatus.FAILED) {
                    emitter.complete();
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });

    }


    @Override
    public void push(UUID fileAssetId, String documentStatus, String message) {
        List<SseEmitter> list = emitters.get(fileAssetId);

        if (list == null || list.isEmpty()) {
            return;
        }

        DocumentStatus newStatus = DocumentStatus.valueOf(documentStatus.toUpperCase());

        push(
                fileAssetId, newStatus, message, list
        );
    }

//    public void push(UUID fileAssetId, DocumentStatus documentStatus, String message)
//    {
//        List<SseEmitter> list = emitters.getOrDefault(fileAssetId, new CopyOnWriteArrayList<>());
//        DocumentStatusEvent event = new DocumentStatusEvent(
//                fileAssetId, documentStatus, message, Instant.now()
//        );
//        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
//        list.forEach(emitter -> {
//            try
//            {
//                emitter.send(SseEmitter.event()
//                        .name("document-status")
//                        .data(event, MediaType.APPLICATION_JSON));
//                if (documentStatus == DocumentStatus.READY || documentStatus == DocumentStatus.FAILED){
//                    emitter.complete();
//                    deadEmitters.add(emitter);
//                }
//            } catch (IOException e)
//            {
//                deadEmitters.add(emitter);
//            }
//        });
//        list.removeAll(deadEmitters);
//    }

    private void removeEmitter(UUID fileAssetId, SseEmitter sseEmitter)
    {
//        List<SseEmitter> list = emitters.get(fileAssetId);
//        if (list != null)
//        {
//            list.remove(sseEmitter);
//            log.info("Remove emitter {} ", sseEmitter);
//            if (list.isEmpty()) {
//                emitters.remove(fileAssetId);
//            }
//        }
        emitters.computeIfPresent(fileAssetId, (key, list) -> {
            list.remove(sseEmitter);             // Удаляем конкретное соединение
            return list.isEmpty() ? null : list; // Если вернули null, мапа сама удалит ключ
        });

    }
}
