CREATE SCHEMA IF NOT EXISTS content;

CREATE TABLE content.documents
(
    id           UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    filename     VARCHAR(255)                NOT NULL,
    content_type VARCHAR(255),
    minio_bucket VARCHAR(255)                NOT NULL,
    minio_path   VARCHAR(255)                NOT NULL,
    file_size    BIGINT,
    uploaded_by  UUID,
    CONSTRAINT pk_documents PRIMARY KEY (id)
);