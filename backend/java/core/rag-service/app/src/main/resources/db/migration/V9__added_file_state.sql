ALTER TABLE content.file_assets
    ADD state_message VARCHAR(255);

ALTER TABLE content.file_assets
    ADD CONSTRAINT uc_file_assets_hash UNIQUE (hash);

ALTER TABLE content.file_assets
    ALTER COLUMN status TYPE VARCHAR(255) USING (status::VARCHAR(255));

ALTER TABLE content.query_logs
    ALTER COLUMN status TYPE VARCHAR(255) USING (status::VARCHAR(255));