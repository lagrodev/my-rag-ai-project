CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE content.document_section
(
    id                UUID                        NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    file_asset_id     UUID                        NOT NULL,
    parent_section_id UUID,
    heading_title     VARCHAR(255),
    heading_depth     INTEGER,
    content           TEXT,
    sequence_number   INTEGER,
    path              LTREE,
    CONSTRAINT pk_documentsection PRIMARY KEY (id),
    CONSTRAINT fk_documentsection_on_parent_section FOREIGN KEY (parent_section_id) REFERENCES content.document_section (id)
);

ALTER TABLE content.document_chunks
    ADD COLUMN chunk_index INTEGER,
    ADD COLUMN document_section_id UUID,
DROP COLUMN file_asset_id;

ALTER TABLE content.document_chunks
    ADD CONSTRAINT fk_document_chunks_on_document_section
        FOREIGN KEY (document_section_id) REFERENCES content.document_section (id);