package com.researchtrace.backend.dto;

public class DeletionImpact {

    private String title;
    private String message;
    private boolean blocked;
    private int tasks;
    private int taskSuggestions;
    private int extractionReviews;
    private int acceptedReviews;
    private int researchDecisions;
    private int experiments;
    private int results;
    private int researchEvents;
    private int researchChanges;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public int getTasks() {
        return tasks;
    }

    public void setTasks(int tasks) {
        this.tasks = tasks;
    }

    public int getTaskSuggestions() {
        return taskSuggestions;
    }

    public void setTaskSuggestions(int taskSuggestions) {
        this.taskSuggestions = taskSuggestions;
    }

    public int getExtractionReviews() {
        return extractionReviews;
    }

    public void setExtractionReviews(int extractionReviews) {
        this.extractionReviews = extractionReviews;
    }

    public int getAcceptedReviews() {
        return acceptedReviews;
    }

    public void setAcceptedReviews(int acceptedReviews) {
        this.acceptedReviews = acceptedReviews;
    }

    public int getResearchDecisions() {
        return researchDecisions;
    }

    public void setResearchDecisions(int researchDecisions) {
        this.researchDecisions = researchDecisions;
    }

    public int getExperiments() {
        return experiments;
    }

    public void setExperiments(int experiments) {
        this.experiments = experiments;
    }

    public int getResults() {
        return results;
    }

    public void setResults(int results) {
        this.results = results;
    }

    public int getResearchEvents() {
        return researchEvents;
    }

    public void setResearchEvents(int researchEvents) {
        this.researchEvents = researchEvents;
    }

    public int getResearchChanges() {
        return researchChanges;
    }

    public void setResearchChanges(int researchChanges) {
        this.researchChanges = researchChanges;
    }
}
