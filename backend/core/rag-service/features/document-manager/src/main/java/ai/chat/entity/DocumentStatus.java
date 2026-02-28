package ai.chat.entity;

/**
 * Статусная машина обработки документа в RAG пайплайне.
 *
 * Жизненный цикл:
 * UPLOADED → SENT_TO_PARSER → PARSING → PARSED → INDEXING → READY
 *                                              ↘               ↘
 *                                           FAILED           FAILED
 */
public enum DocumentStatus {

    /** Файл загружен в MinIO, ещё не отправлен в очередь на обработку */
    UPLOADED,

    /** Событие отправлено в Kafka — воркер должен начать парсинг */
    SENT_TO_PARSER,

    /** Воркер парсит документ: извлекает секции и их содержимое */
    PARSING,

    /** Парсинг завершён, секции сохранены в БД */
    PARSED,

    /** Секции разбиваются на чанки и генерируются эмбеддинги */
    INDEXING,

    /** Документ полностью готов к поиску */
    READY,

    /** Произошла ошибка на любом этапе, причина в failureReason */
    FAILED
}
