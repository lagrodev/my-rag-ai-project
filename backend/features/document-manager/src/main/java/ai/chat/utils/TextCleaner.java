package ai.chat.utils;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class TextCleaner{
    public String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // Шаг 1: Нормализация юникода (приводит разные кодировки букв к одному стандарту)
        text = Normalizer.normalize(text, Normalizer.Form.NFC);

        // Шаг 2: Удаление невидимого мусора (Zero-width spaces, BOM и прочая дичь из PDF)
        text = text.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");

        // Шаг 3: Склеивание переносов словTypeScript
        // Если слово разорвано: "дого-\nвор" -> делаем "договор"
        text = text.replaceAll("([а-яА-Яa-zA-Z]+)-\\s*\\n\\s*([а-яА-Яa-zA-Z]+)", "$1$2");

        // Шаг 4: Убийство номеров страниц (одиночные цифры на пустой строке)
        text = text.replaceAll("(?m)^\\s*\\d+\\s*$", "");

        // Шаг 5: Очистка от колонтитулов (настрой под свои доки, это пример)
        text = text.replaceAll("(?i)(?m)^\\s*(страница \\d+ из \\d+|конфиденциально)\\s*$", "");

        // Шаг 6: Нормализация пробелов и пустых строк (САМОЕ ВАЖНОЕ)
        // Убираем множественные пробелы внутри строки (оставляем один)
        text = text.replaceAll("[ \\t]{2,}", " ");
        // Если есть 3 и более переносов строки подряд, сжимаем их до двух (сохраняем абзац)
        text = text.replaceAll("\\n{3,}", "\n\n");

        // Шаг 7: Фильтрация строк-мусора (например, строки состоящие только из точек или спецсимволов)
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                // Оставляем пустые строки (для разделения абзацев) ИЛИ строки, где есть буквы/цифры
                .filter(line -> line.isEmpty() || line.matches(".*[a-zA-Zа-яА-Я0-9].*"))
                .collect(Collectors.joining("\n"))
                .trim();
    }
}
