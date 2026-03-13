package ai.chat.entity;


import java.time.LocalDateTime;

public enum DocumentStatus
{

    /** Файл загружен в MinIO, ещё не отправлен в очередь на обработку */
    UPLOADED,
    /** Файл улетел и принят питоном */
    PROCESSING,

    /** питон сделал свою работу */
    MARKDOWN_READY,


    /** Событие отправлено в Kafka — воркер должен начать парсинг */
    SENT_TO_PARSER,

    /** Воркер парсит документ: извлекает секции и их содержимое */
    PARSING,


    /** Парсинг завершён, секции сохранены в БД */
    PARSED,

    /** Секции разбиваются на чанки и генерируются эмбеддинги */
     INDEXING,
    READY {
        @Override
        public void applySpecificFields(FileAsset asset, String message) {
            asset.setProcessedAt(LocalDateTime.now());
        }
    },
    FAILED {
        @Override
        public void applySpecificFields(FileAsset asset, String message) {
            asset.setFailureReason(message);
            asset.setProcessedAt(LocalDateTime.now());
        }
    };

    public void applySpecificFields(FileAsset asset, String message) {

    }
}
