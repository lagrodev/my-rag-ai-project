import asyncio
import json
import structlog
from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from src.domain.models import IncomingEvent
from src.infrastructure.minio.storage import MinioStorage
from src.infrastructure.pdf.processor import PDFProcessor
from src.application.service import DocumentProcessingService

logger = structlog.get_logger()

# В реальности берите из pydantic-settings (config.py)
KAFKA_BROKER = "kafka:9092"
IN_TOPIC = "pdf-incoming"
OUT_TOPIC = "json-outgoing"
CONSUMER_GROUP = "pdf-processor-group"


class KafkaProducerWrapper:
    def __init__(self, producer: AIOKafkaProducer, topic: str):
        self.producer = producer
        self.topic = topic

    async def send_message(self, message: str):
        await self.producer.send_and_wait(self.topic, message.encode('utf-8'))


async def main():
    # Инициализация инфраструктуры
    storage = MinioStorage(endpoint="minio:9000", access_key="minioadmin", secret_key="minioadmin")
    pdf_processor = PDFProcessor()

    producer = AIOKafkaProducer(bootstrap_servers=KAFKA_BROKER)
    await producer.start()

    producer_wrapper = KafkaProducerWrapper(producer, OUT_TOPIC)
    service = DocumentProcessingService(storage, pdf_processor, producer_wrapper)

    # Настройка consumer (At-Least-Once семантика: enable_auto_commit=False)
    consumer = AIOKafkaConsumer(
        IN_TOPIC,
        bootstrap_servers=KAFKA_BROKER,
        group_id=CONSUMER_GROUP,
        enable_auto_commit=False,
        value_deserializer=lambda m: json.loads(m.decode('utf-8'))
    )
    await consumer.start()

    logger.info("service_started")

    try:
        async for msg in consumer:
            try:
                event = IncomingEvent(**msg.value)
                await service.process_event(event)
                # Коммитим оффсет ТОЛЬКО после успешной обработки или осознанного скипа (At-Least-Once)
                await consumer.commit()
            except Exception as e:
                logger.error("error_processing_message", error=str(e), partition=msg.partition, offset=msg.offset)
                # Если падает тут, оффсет не коммитится. При перезапуске сообщение прочитается снова.
    finally:
        await consumer.stop()
        await producer.stop()


if __name__ == "__main__":
    asyncio.run(main())