"""Application Service — оркестрация обработки PDF-документов.

Отвечает за:
1. Idempotency-проверку (пропуск уже обработанных документов)
2. Скачивание PDF из MinIO
3. Конвертацию PDF → Markdown (с очисткой)
4. Сохранение JSON-результата в MinIO
5. Отправку Kafka-события о завершении
6. Очистку временных файлов
"""

import asyncio
import os
from pathlib import Path

import structlog

from src.core.config import settings
from src.domain.models import (
    IncomingEvent,
    OutgoingEvent,
    ProcessedDocument,
)

from src.infrastructure.pdf.tree_builder import MarkdownTreeBuilder
from src.infrastructure.kafka.producer import KafkaEventProducer
from src.infrastructure.minio.storage import MinioStorage
from src.infrastructure.pdf.processor import PDFProcessor
from minio.error import S3Error

logger = structlog.get_logger()


class DocumentProcessingService:
    """Сервис обработки PDF-документов (Application layer)."""

    def __init__(
        self,
        storage: MinioStorage,
        pdf_processor: PDFProcessor,
        tree_builder: MarkdownTreeBuilder,
        producer: KafkaEventProducer,
    ) -> None:
        self._storage = storage
        self._pdf = pdf_processor
        self._tree_builder = tree_builder
        self._producer = producer

    async def process_event(self, event: IncomingEvent) -> None:
        """Полный pipeline обработки одного входящего события."""

        log = logger.bind(document_id=event.id)
        output_bucket = settings.minio_output_bucket
        output_object = f"{event.id}.json"

        # ── 1. Idempotency: проверяем, не обработан ли уже ────────
        already_exists = await self._storage.object_exists(
            output_bucket, output_object
        )
        if already_exists:
            log.info(
                "document_already_processed",
                result=f"minio://{output_object}",
            )
            return

        # ── 2. Скачиваем PDF из MinIO ─────────────────────────────
        bucket, object_name = self._parse_minio_path(
            event.filePath, event.minIoBucket
        )

        temp_dir = Path(settings.temp_dir)
        temp_dir.mkdir(parents=True, exist_ok=True)
        local_pdf = str(temp_dir / f"{event.id}.pdf")

        try:
            try:
                await self._storage.download_file(bucket, object_name, local_pdf)
            except S3Error as exc:
                if exc.code == "NoSuchKey":
                    log.warning("file_missing_in_minio_skipping", object_name=object_name)
                    return
                raise

            # ── 3. PDF → очищенный Markdown (по страницам) ────────
            page_chunks = await asyncio.to_thread(
                self._pdf.convert_to_markdown, local_pdf
            )

            tree_data = await asyncio.to_thread(
                self._tree_builder.build_tree, page_chunks
            )

            # ── 4. Формируем JSON-документ и загружаем в MinIO ────
            doc = ProcessedDocument(
                document_id=event.id,
                source_file=event.filePath,
                tree=tree_data,
            )

            await self._storage.upload_json(
                output_bucket, output_object, doc.to_storage_dict()
            )

            # ── 5. Отправляем Kafka-событие ───────────────────────
            out_event = OutgoingEvent(
                document_id=event.id,
                result_bucket=output_bucket,
                result_file=f"{output_object}",
                status="processed",
            )
            await self._producer.send_event(out_event)

            log.info(
                "document_processed_ok",
                result=f"minio://{output_bucket}/{output_object}",
            )

        finally:
            # ── 6. Очистка временных файлов ───────────────────────
            self._cleanup(local_pdf)

    # ── Вспомогательные методы ────────────────────────────────────
    @staticmethod
    def _parse_minio_path(
            file_path: str, fallback_bucket: str
    ) -> tuple[str, str]:

        prefix = "minio://"
        if file_path.startswith(prefix):
            rest = file_path[len(prefix):]
            parts = rest.split("/", 1)
            if len(parts) == 2:
                return parts[0], parts[1]


        if file_path.startswith(f"{fallback_bucket}/"):
            clean_object_name = file_path[len(fallback_bucket) + 1:]
            return fallback_bucket, clean_object_name
        # ----------------------

        return fallback_bucket, file_path

    @staticmethod
    def _cleanup(path: str) -> None:
        try:
            if os.path.exists(path):
                os.remove(path)
        except OSError:
            logger.warning("temp_file_cleanup_failed", path=path)