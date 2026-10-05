package com.researchtrace.backend.ai;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Deterministic extractor for tests and offline demos. It only reads explicit lines of the form:
 *
 * <pre>
 * ACTION: assignee | title | yyyy-MM-dd | milestone title
 * TODO:   assignee | title | yyyy-MM-dd | milestone title
 * </pre>
 *
 * Only the title is required. A line with a single segment is treated as the title.
 */
@Component
public class StubTaskExtractor implements TaskExtractor {

    private static final List<String> PREFIXES = List.of("ACTION:", "TODO:");
    private static final int MAX_SEGMENTS = 4;
    private static final BigDecimal EXPLICIT_LINE_CONFIDENCE = new BigDecimal("1.00");

    @Override
    public List<ExtractedTask> extract(TaskExtractionInput input) {
        if (input == null || input.notes() == null) {
            return List.of();
        }

        List<ExtractedTask> tasks = new ArrayList<>();
        for (String rawLine : input.notes().split("\\R")) {
            String line = rawLine.trim();
            String body = stripPrefix(line);
            if (body == null) {
                continue;
            }

            ExtractedTask task = parse(body, line, input);
            if (task != null) {
                tasks.add(task);
            }
        }
        return List.copyOf(tasks);
    }

    private String stripPrefix(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        for (String prefix : PREFIXES) {
            if (upper.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private ExtractedTask parse(String body, String line, TaskExtractionInput input) {
        String[] segments = body.split("\\|", -1);
        if (segments.length > MAX_SEGMENTS) {
            return null;
        }

        String assigneeName;
        String title;
        if (segments.length == 1) {
            assigneeName = null;
            title = blankToNull(segments[0]);
        } else {
            assigneeName = blankToNull(segments[0]);
            title = blankToNull(segments[1]);
        }
        if (title == null) {
            return null;
        }

        LocalDate dueDate = segments.length >= 3 ? parseDate(segments[2]) : null;
        String milestoneTitle = segments.length == 4 ? blankToNull(segments[3]) : null;

        return new ExtractedTask(
            title,
            null,
            assigneeName,
            uniqueIdByName(input.students(), assigneeName,
                TaskExtractionInput.StudentCandidate::name,
                TaskExtractionInput.StudentCandidate::id),
            uniqueIdByName(input.milestones(), milestoneTitle,
                TaskExtractionInput.MilestoneCandidate::title,
                TaskExtractionInput.MilestoneCandidate::id),
            dueDate,
            line,
            EXPLICIT_LINE_CONFIDENCE);
    }

    private LocalDate parseDate(String value) {
        String date = blankToNull(value);
        if (date == null) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private <T> Long uniqueIdByName(
            List<T> candidates,
            String name,
            Function<T, String> nameOf,
            Function<T, Long> idOf) {

        if (name == null) {
            return null;
        }

        Long match = null;
        for (T candidate : candidates) {
            String candidateName = nameOf.apply(candidate);
            if (candidateName != null && candidateName.trim().equalsIgnoreCase(name)) {
                if (match != null) {
                    return null;
                }
                match = idOf.apply(candidate);
            }
        }
        return match;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
