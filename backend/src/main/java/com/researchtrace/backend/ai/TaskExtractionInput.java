package com.researchtrace.backend.ai;

import java.time.LocalDate;
import java.util.List;

public record TaskExtractionInput(
        String meetingTitle,
        LocalDate meetingDate,
        String notes,
        List<StudentCandidate> students,
        List<MilestoneCandidate> milestones) {

    public TaskExtractionInput {
        students = students == null ? List.of() : List.copyOf(students);
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
    }

    public record StudentCandidate(Long id, String name) {
    }

    public record MilestoneCandidate(Long id, String title) {
    }
}
