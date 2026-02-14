package ai.chat.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

@Converter
public class VectorConverter implements AttributeConverter<List<Double>, String> {

    @Override
    public String convertToDatabaseColumn(List<Double> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public List<Double> convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        String cleaned = s.replaceAll("[\\[\\]\\s]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        return Arrays.stream(
                cleaned.split(","))
                .map(String::trim)
                .map(Double::valueOf)
                .toList();
    }
}
