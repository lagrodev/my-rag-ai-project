"""PDF → Markdown конвертер и очистка (RAG-оптимизированный).

Использует pymupdf4llm с ``page_chunks=True`` — каждая страница
возвращается отдельным словарём с метаданными (номер страницы),
что критически важно для source-citations в RAG.

Колонтитулы отсекаются через ``margins`` (верхние/нижние 12 % страницы),
а не текстовыми регулярками.  Порядок кортежа — PyMuPDF-геометрия:
``(left, top, right, bottom)``.

Оглавление НЕ удаляется: для RAG оно содержит ключевые слова
и помогает ретриверу понять структуру документа.

Алгоритм очистки каждой страницы:
1. Замена Unicode-артефактов (NBSP, zero-width space и пр.).
2. Удаление одиночных номеров страниц (только чистые «-3-», « 12 »,
   НЕ годы 1900–2099).
3. Удаление горизонтальных разделителей (---/===/___ и т.п.).
4. Схлопывание множественных пустых строк до одной.
"""

from __future__ import annotations

import re
from typing import Any

import fitz  # PyMuPDF
import pymupdf4llm
import structlog

logger = structlog.get_logger()

# ── Паттерны ─────────────────────────────────────────────────────

# Номер страницы: одиночное число 1–9999, возможно обрамлённое тире.
# Negative lookahead исключает годы 1900–2099.
_RE_PAGE_NUMBER = re.compile(
    r"^\s*[-—–]?\s*(?!(?:19|20)\d{2}\s*$)\d{1,4}\s*[-—–]?\s*$",
    re.MULTILINE,
)

_RE_HORIZONTAL_RULE = re.compile(
    r"^\s*[-=_*]{3,}\s*$", re.MULTILINE
)
_RE_MULTIPLE_BLANK = re.compile(r"\n{3,}")
_RE_UNICODE_JUNK = re.compile(
    r"[\u00a0\u200b\u200c\u200d\ufeff\u2028\u2029]"
)

# Доля высоты страницы, отсекаемая сверху и снизу (колонтитулы)
_MARGIN_RATIO = 0.12


class PageChunk:
    """Результат обработки одной страницы PDF."""

    __slots__ = ("page", "text")

    def __init__(self, page: int, text: str) -> None:
        self.page = page
        self.text = text

    def to_dict(self) -> dict[str, Any]:
        return {"page": self.page, "text": self.text}


class PDFProcessor:
    """Конвертирует PDF в список очищенных Markdown-чанков по страницам."""

    def convert_to_markdown(self, pdf_path: str) -> list[dict[str, Any]]:
        """PDF → список ``{"page": N, "text": "..."}`` по страницам.

        Документ открывается через ``fitz.open()`` один раз и передаётся
        в ``pymupdf4llm.to_markdown()`` как готовый объект, чтобы
        избежать тройного чтения файла с диска.

        Returns:
            Список словарей с ключами ``page`` (int) и ``text`` (str).

        Raises:
            RuntimeError: если pymupdf4llm / PyMuPDF не смог прочитать файл.
        """
        doc: fitz.Document | None = None
        try:
            logger.info("pdf_conversion_start", path=pdf_path)

            # ── Открываем PDF один раз ────────────────────────────
            doc = fitz.open(pdf_path)

            # ── Вычисляем margins в pt ────────────────────────────
            first_page = doc[0]
            margin_top = first_page.rect.height * _MARGIN_RATIO
            margin_bottom = first_page.rect.height * _MARGIN_RATIO

            # ── Конвертация с page_chunks=True ────────────────────
            # Передаём fitz.Document напрямую — без повторного чтения файла.
            # Порядок margins — PyMuPDF-геометрия: (left, top, right, bottom).
            raw_chunks: list[dict] = pymupdf4llm.to_markdown(
                doc,
                page_chunks=True,
                margins=(0, margin_top, 0, margin_bottom),
            )

            # ── Очистка каждой страницы ───────────────────────────
            result: list[PageChunk] = []
            for chunk in raw_chunks:
                page_num: int = chunk["metadata"]["page"]

                cleaned = self._clean_page(chunk["text"])
                if cleaned:
                    result.append(PageChunk(page=page_num, text=cleaned))

            logger.info(
                "pdf_conversion_ok",
                path=pdf_path,
                total_pages=len(raw_chunks),
                result_pages=len(result),
            )
            return [c.to_dict() for c in result]

        except Exception as exc:
            logger.error(
                "pdf_conversion_failed", error=str(exc), path=pdf_path
            )
            raise RuntimeError(
                f"Не удалось сконвертировать PDF: {exc}"
            ) from exc
        finally:
            if doc is not None:
                doc.close()

    # ── Очистка одной страницы ────────────────────────────────────

    @staticmethod
    def _clean_page(text: str) -> str:
        """Безопасная очистка Markdown одной страницы.

        Не трогает таблицы и блоки кода — работает только
        с заведомо мусорными паттернами.
        """

        # 1. Unicode-артефакты → пробел
        text = _RE_UNICODE_JUNK.sub(" ", text)

        # 2. Номера страниц (не годы)
        text = _RE_PAGE_NUMBER.sub("", text)

        # 3. Горизонтальные разделители
        text = _RE_HORIZONTAL_RULE.sub("", text)

        # 4. Множественные пустые строки
        text = _RE_MULTIPLE_BLANK.sub("\n\n", text)

        return text.strip()