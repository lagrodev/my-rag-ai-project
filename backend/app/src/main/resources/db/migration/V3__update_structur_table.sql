CREATE TABLE content.file_assets
(
    id           UUID         NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    hash         VARCHAR(255) NOT NULL,
    minio_bucket VARCHAR(255) NOT NULL,
    minio_path   VARCHAR(255) NOT NULL,
    file_size    BIGINT,
    content_type VARCHAR(255),
    is_parsed    BOOLEAN      NOT NULL,
    CONSTRAINT pk_file_assets PRIMARY KEY (id)
);

CREATE TABLE content.outbox_events
(
    id         UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    asset_id   UUID,
    type       VARCHAR(255),
    payload    JSONB,
    state      VARCHAR(255),
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);
