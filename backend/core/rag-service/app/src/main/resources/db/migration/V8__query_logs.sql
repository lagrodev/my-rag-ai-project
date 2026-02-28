-- Таблица для логирования RAG запросов (observability / quality evaluation)
CREATE TABLE content.query_logs
(
    id                     UUID                        NOT NULL,
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITHOUT TIME ZONE,
    file_asset_id          UUID                        NOT NULL,
    user_id                UUID,
    query                  TEXT                        NOT NULL,
    answer                 TEXT,
    retrieved_chunks_count INTEGER,
    max_similarity_score   DOUBLE PRECISION,
    min_similarity_score   DOUBLE PRECISION,
    sources                JSONB,
    latency_ms             BIGINT,
    status                 VARCHAR(20)                 NOT NULL DEFAULT 'SUCCESS',
    error_message          TEXT,
    queried_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_query_logs PRIMARY KEY (id)
);

CREATE INDEX idx_query_logs_file_asset_id ON content.query_logs (file_asset_id);
CREATE INDEX idx_query_logs_user_id ON content.query_logs (user_id);
CREATE INDEX idx_query_logs_queried_at ON content.query_logs (queried_at DESC);
CREATE INDEX idx_query_logs_status ON content.query_logs (status);
