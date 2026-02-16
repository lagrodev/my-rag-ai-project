"""doc-parser-python — точка входа.

Запускает Kafka consumer, который слушает pdf-incoming topic,
обрабатывает PDF → Markdown → JSON и публикует результат.
"""

import asyncio
import signal

import structlog

from src.core.config import settings
from src.core.logger import setup_logging
from src.application.service import DocumentProcessingService
from src.infrastructure.kafka.consumer import KafkaEventConsumer
from src.infrastructure.kafka.producer import KafkaEventProducer
from src.infrastructure.minio.storage import MinioStorage
from src.infrastructure.pdf.processor import PDFProcessor

setup_logging()
logger = structlog.get_logger()


async def main() -> None:
    """Инициализация инфраструктуры и запуск consumer-цикла."""

    logger.info(
        "service_starting",
        kafka=settings.kafka_bootstrap_servers,
        minio=settings.minio_endpoint,
        input_topic=settings.kafka_input_topic,
        output_topic=settings.kafka_output_topic,
    )

    # ── Инфраструктурные компоненты ───────────────────────────────
    storage = MinioStorage()
    pdf_processor = PDFProcessor()
    producer = KafkaEventProducer()
    consumer = KafkaEventConsumer()

    # ── Application service ───────────────────────────────────────
    service = DocumentProcessingService(storage, pdf_processor, producer)

    # ── Graceful shutdown ─────────────────────────────────────────
    loop = asyncio.get_running_loop()
    shutdown_event = asyncio.Event()

    def _signal_handler() -> None:
        logger.info("shutdown_signal_received")
        shutdown_event.set()

    try:
        for sig in (signal.SIGINT, signal.SIGTERM):
            loop.add_signal_handler(sig, _signal_handler)
    except NotImplementedError:
        # Windows не поддерживает add_signal_handler — KeyboardInterrupt
        # перехватывается через try/except в consumer loop
        pass

    # ── Запуск ────────────────────────────────────────────────────
    await producer.start()
    await consumer.start()

    logger.info("service_started")

    # Запускаем consumer loop и shutdown watcher параллельно
    consumer_task = asyncio.create_task(
        consumer.consume(service.process_event)
    )
    shutdown_task = asyncio.create_task(shutdown_event.wait())

    done, pending = await asyncio.wait(
        {consumer_task, shutdown_task},
        return_when=asyncio.FIRST_COMPLETED,
    )

    # ── Остановка ─────────────────────────────────────────────────
    for task in pending:
        task.cancel()

    await consumer.stop()
    await producer.stop()
    logger.info("service_stopped")


if __name__ == "__main__":
    asyncio.run(main())