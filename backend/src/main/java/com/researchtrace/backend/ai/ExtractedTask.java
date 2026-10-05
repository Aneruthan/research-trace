package com.researchtrace.backend.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExtractedTask(
        String title,
        String description,
        String assigneeName,
        Long assigneeId,
        Long milestoneId,
        LocalDate dueDate,
        String sourceExcerpt,
        BigDecimal confidence) {
}
