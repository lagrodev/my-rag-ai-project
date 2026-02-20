

ALTER TABLE content.documents
    ADD file_asset_id UUID;

ALTER TABLE content.documents
    ALTER COLUMN file_asset_id SET NOT NULL;

ALTER TABLE content.documents
    ADD CONSTRAINT FK_DOCUMENTS_ON_FILE_ASSET FOREIGN KEY (file_asset_id) REFERENCES content.file_assets (id);

ALTER TABLE content.documents
DROP COLUMN content_type;

ALTER TABLE content.documents
DROP COLUMN file_size;

ALTER TABLE content.documents
DROP COLUMN minio_bucket;

ALTER TABLE content.documents
DROP COLUMN minio_path;