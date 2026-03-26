-- Статусная машина для обработки документов в RAG пайплайне
-- Статусы: UPLOADED → SENT_TO_PARSER → PARSING → PARSED → INDEXING → READY / FAILED

ALTER TABLE content.file_assets
    ADD COLUMN IF NOT EXISTS status        VARCHAR(50)  NOT NULL DEFAULT 'UPLOADED',
    ADD COLUMN IF NOT EXISTS failure_reason TEXT,
    ADD COLUMN IF NOT EXISTS processed_at  TIMESTAMP WITHOUT TIME ZONE;

-- Синхронизируем статус на основе существующего флага is_parsed
UPDATE content.file_assets
SET status = 'READY'
WHERE is_parsed = TRUE
  AND status = 'UPLOADED';


CREATE INDEX ON content.document_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
