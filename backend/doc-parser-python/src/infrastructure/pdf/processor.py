import pymupdf4llm
import re
import structlog
from pathlib import Path

logger = structlog.get_logger()


class PDFProcessor:
    def convert_to_markdown(self, pdf_path: str) -> str:
        try:
            logger.info("converting_pdf_to_md", path=pdf_path)
            # pymupdf4llm делает почти всю магию за нас
            md_text = pymupdf4llm.to_markdown(pdf_path)
            return self._clean_markdown(md_text)
        except Exception as e:
            logger.error("pdf_conversion_failed", error=str(e), path=pdf_path)
            raise

    def _clean_markdown(self, text: str) -> str:
        # 1. Удаление оглавления (строки типа "Введение ....... 5")
        text = re.sub(r'^.*?\.{4,}.*\d+$', '', text, flags=re.MULTILINE)

        # 2. Удаление одиночных номеров страниц (например, " 12 " на отдельной строке)
        text = re.sub(r'^\s*-?\d+-?\s*$', '', text, flags=re.MULTILINE)

        # 3. Удаление артефактов из повторяющихся хедеров (если они известны, можно добавить)
        # 4. Убираем лишние переносы строк (больше двух подряд)
        text = re.sub(r'\n{3,}', '\n\n', text)

        return text.strip()