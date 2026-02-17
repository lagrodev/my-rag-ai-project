from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field
from datetime import datetime, timezone


class IncomingEvent(BaseModel):
    """Входящее Kafka-событие с информацией о PDF-файле в MinIO."""

    id: str
    minIoBucket: str = Field(..., alias="minIoBucket")
    filePath: str

    model_config = {"populate_by_name": True}


class PageChunkModel(BaseModel):
    """Одна страница PDF после конвертации и очистки."""

    page: int
    text: str


class ProcessedDocument(BaseModel):
    """JSON-документ, сохраняемый в MinIO после обработки.

    ``pages`` — список чанков по страницам с номером страницы,
    что позволяет RAG-системе давать source-citations.
    """

    document_id: str
    source_file: str
    pages: list[PageChunkModel]
    processed_at: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc)
    )

    def to_storage_dict(self) -> dict[str, Any]:
        return {
            "document_id": self.document_id,
            "source_file": self.source_file,
            "pages": [p.model_dump() for p in self.pages],
            "processed_at": self.processed_at.isoformat(),
        }


class OutgoingEvent(BaseModel):
    """Исходящее Kafka-событие после успешной обработки."""

    document_id: str
    result_bucket: str
    result_file: str
    status: str = "processed"