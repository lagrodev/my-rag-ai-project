CREATE TABLE content.document_chunks
(
    id          UUID                         NOT NULL primary key,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    document_id UUID NOT NULL REFERENCES content.documents(id) ON DELETE CASCADE,
    content     TEXT                        NOT NULL,
    embedding   VECTOR(1536)                   ,
    CONSTRAINT pk_document_chunks PRIMARY KEY (id)
);
CREATE INDEX ON content.document_chunks USING hnsw (embedding vector_cosine_ops);

