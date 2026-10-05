package com.researchtrace.backend.dto;

import java.time.LocalDate;

/**
 * Reviewed values for a task suggestion. A null field keeps the suggestion's current value.
 */
public record AcceptTaskSuggestionRequest(
        String title,
        String description,
        Long assigneeId,
        Long milestoneId,
        LocalDate dueDate) {
}
