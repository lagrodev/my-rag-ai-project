import io
import json
import asyncio
from minio import Minio
from minio.error import S3Error
from tenacity import retry, stop_after_attempt, wait_exponential
import structlog

logger = structlog.get_logger()


class MinioStorage:
    def __init__(self, endpoint, access_key, secret_key, secure=False):
        self.client = Minio(endpoint, access_key=access_key, secret_key=secret_key, secure=secure)

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
    async def download_file(self, bucket_name: str, object_name: str, file_path: str):
        logger.info("downloading_from_minio", bucket=bucket_name, object=object_name)
        await asyncio.to_thread(self.client.fget_object, bucket_name, object_name, file_path)

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
    async def upload_json(self, bucket_name: str, object_name: str, data: dict):
        logger.info("uploading_to_minio", bucket=bucket_name, object=object_name)

        # Проверяем/создаем корзину
        exists = await asyncio.to_thread(self.client.bucket_exists, bucket_name)
        if not exists:
            await asyncio.to_thread(self.client.make_bucket, bucket_name)

        json_bytes = json.dumps(data, ensure_ascii=False).encode('utf-8')
        json_stream = io.BytesIO(json_bytes)

        await asyncio.to_thread(
            self.client.put_object,
            bucket_name, object_name, json_stream, length=len(json_bytes), content_type="application/json"
        )

    async def check_exists(self, bucket_name: str, object_name: str) -> bool:
        """Для идемпотентности"""
        try:
            await asyncio.to_thread(self.client.stat_object, bucket_name, object_name)
            return True
        except S3Error as e:
            if e.code == 'NoSuchKey':
                return False
            raise