ALTER TABLE content.document_chunks
DROP
CONSTRAINT document_chunks_document_id_fkey;

ALTER TABLE content.document_chunks
    ADD file_asset_id UUID;

ALTER TABLE content.document_chunks
    ALTER COLUMN file_asset_id SET NOT NULL;


ALTER TABLE content.document_chunks
DROP
COLUMN document_id;