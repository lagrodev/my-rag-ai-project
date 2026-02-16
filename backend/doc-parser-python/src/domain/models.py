from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional

class IncomingEvent(BaseModel):
    id: str
    minIoBucket: str
    filePath: str

class ProcessedDocument(BaseModel):
    document_id: str
    source_file: str
    content_markdown: str
    processed_at: datetime = Field(default_factory=datetime.utcnow)

class OutgoingEvent(BaseModel):
    document_id: str
    result_bucket: str
    result_file: str
    status: str