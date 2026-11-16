package com.researchtrace.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Google Gemini implementation of TaskExtractor using Spring AI.
 * Extracts candidate tasks from meeting notes as plain data.
 * Validates assignee and milestone candidates without inventing IDs.
 */
public class GeminiTaskExtractor implements TaskExtractor {

    public record GeminiTaskDto(
            String title,
            String description,
            String assigneeName,
            Long assigneeId,
            Long milestoneId,
            String dueDate,
            String sourceExcerpt,
            Double confidence
    ) {}

    public record GeminiTaskExtractionResponse(
            List<GeminiTaskDto> tasks
    ) {}

    static final String SYSTEM_PROMPT = """
            You are an AI assistant for a research project supervision system.
            Extract actionable research-project tasks from meeting notes.

            Return JSON matching the schema with a list of tasks.
            For each task, provide:
            - title: Clear, concise summary of the task (required).
            - description: Elaborated details of the action item, or null.
            - assigneeName: Name of the assigned student, or null.
            - assigneeId: Exact numeric ID of one of the provided student candidates, or null if unassigned or ambiguous. Never invent an ID.
            - milestoneId: Exact numeric ID of one of the provided milestone candidates, or null if unassigned or ambiguous.
            - dueDate: Exact deadline in ISO-8601 format (YYYY-MM-DD), or null. If ambiguous, relative without certainty, or uncertain, set dueDate to null and note the timing in description.
            - sourceExcerpt: A direct short quote copied from the meeting notes supporting this task, or null.
            - confidence: Confidence value between 0.0 and 1.0.

            Rules:
            - Include only tasks clearly supported by the notes.
            - Never invent student IDs or milestone IDs outside the candidates provided.
            - Do not create, update, or delete any database records.
            """;

    private static final Logger log = LoggerFactory.getLogger(GeminiTaskExtractor.class);

    private final ChatClient chatClient;

    public GeminiTaskExtractor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public List<ExtractedTask> extract(TaskExtractionInput input) {
        if (input == null || input.notes() == null || input.notes().isBlank()) {
            return List.of();
        }

        String userPrompt = buildUserPrompt(input);

        GeminiTaskExtractionResponse response;
        try {
            response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .entity(GeminiTaskExtractionResponse.class);
        } catch (Exception e) {
            log.warn("Gemini task extraction failed ({})", e.getClass().getSimpleName());
            throw new TaskExtractionException("Task extraction failed. The model response could not be used.", e);
        }

        return tasksFromResponse(response, input);
    }

    static List<ExtractedTask> tasksFromResponse(GeminiTaskExtractionResponse response, TaskExtractionInput input) {
        if (response == null || response.tasks() == null) {
            throw new TaskExtractionException("Gemini returned malformed task extraction output");
        }

        Set<Long> validStudentIds = input.students() == null
                ? Set.of()
                : input.students().stream()
                    .map(TaskExtractionInput.StudentCandidate::id)
                    .collect(Collectors.toSet());

        Set<Long> validMilestoneIds = input.milestones() == null
                ? Set.of()
                : input.milestones().stream()
                    .map(TaskExtractionInput.MilestoneCandidate::id)
                    .collect(Collectors.toSet());

        List<ExtractedTask> result = new ArrayList<>();
        for (GeminiTaskDto dto : response.tasks()) {
            if (dto == null) continue;
            String title = trimToNull(dto.title());
            if (title == null) continue;

            String description = trimToNull(dto.description());
            String assigneeName = trimToNull(dto.assigneeName());

            // Safeguard: strictly ensure assigneeId belongs to provided candidates
            Long assigneeId = (dto.assigneeId() != null && validStudentIds.contains(dto.assigneeId()))
                    ? dto.assigneeId()
                    : null;

            // Safeguard: strictly ensure milestoneId belongs to provided candidates
            Long milestoneId = (dto.milestoneId() != null && validMilestoneIds.contains(dto.milestoneId()))
                    ? dto.milestoneId()
                    : null;

            // Parse due date safely, preserving uncertainty
            LocalDate dueDate = parseDueDate(dto.dueDate());

            String sourceExcerpt = trimToNull(dto.sourceExcerpt());

            BigDecimal confidence = null;
            if (dto.confidence() != null && dto.confidence() >= 0.0 && dto.confidence() <= 1.0) {
                confidence = BigDecimal.valueOf(dto.confidence()).setScale(2, RoundingMode.HALF_UP);
            }

            result.add(new ExtractedTask(
                    title,
                    description,
                    assigneeName,
                    assigneeId,
                    milestoneId,
                    dueDate,
                    sourceExcerpt,
                    confidence
            ));
        }

        return List.copyOf(result);
    }

    private String buildUserPrompt(TaskExtractionInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Meeting Title: ").append(input.meetingTitle() != null ? input.meetingTitle() : "Meeting").append("\n");
        sb.append("Meeting Date: ").append(input.meetingDate() != null ? input.meetingDate().toString() : "Not specified").append("\n\n");

        sb.append("Candidate Project Students (use exact IDs if assigned):\n");
        if (input.students() == null || input.students().isEmpty()) {
            sb.append("  (No students registered for this project)\n");
        } else {
            for (TaskExtractionInput.StudentCandidate s : input.students()) {
                sb.append("  - ID: ").append(s.id()).append(", Name: ").append(s.name()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("Candidate Project Milestones (use exact IDs if linked):\n");
        if (input.milestones() == null || input.milestones().isEmpty()) {
            sb.append("  (No milestones created for this project)\n");
        } else {
            for (TaskExtractionInput.MilestoneCandidate m : input.milestones()) {
                sb.append("  - ID: ").append(m.id()).append(", Title: ").append(m.title()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("Meeting Notes:\n");
        sb.append(input.notes()).append("\n");

        return sb.toString();
    }

    private static LocalDate parseDueDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException e) {
            // Ambiguous or non-standard format is left null for human review
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
