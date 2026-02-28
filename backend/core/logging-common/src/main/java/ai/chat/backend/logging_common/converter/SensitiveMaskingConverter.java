package ai.chat.backend.logging_common.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;
import java.util.regex.Pattern;

public class SensitiveMaskingConverter extends ClassicConverter {

    private static final String MASK = "****";

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile(
                    "(?i)(password|passwd|pwd|secret|token|access_token|refresh_token|api[_-]?key)"
                            + "([\"']?\\s*[:=]\\s*[\"']?)([^\"'&,;\\s\\]\\}]+)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "(?i)(Bearer\\s+)([A-Za-z0-9\\-._~+/]+=*)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "(?i)(Basic\\s+)([A-Za-z0-9+/]+=*)",
                    Pattern.CASE_INSENSITIVE)
    );

    @Override
    public String convert(ILoggingEvent event) {
        String msg = event.getFormattedMessage();
        if (msg == null || msg.isEmpty()) return "";

        for (Pattern pattern : PATTERNS) {
            msg = pattern.matcher(msg).replaceAll(m -> {
                if (m.groupCount() == 3) {
                    return escapeReplacement(m.group(1)) + escapeReplacement(m.group(2)) + MASK;
                }
                return escapeReplacement(m.group(1)) + MASK;
            });
        }
        return msg;
    }

    private static String escapeReplacement(String s) {
        return s.replace("\\", "\\\\").replace("$", "\\$");
    }
}
