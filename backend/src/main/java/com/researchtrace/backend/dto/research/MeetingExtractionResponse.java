package com.researchtrace.backend.dto.research;

import java.util.List;

public class MeetingExtractionResponse {

    private List<ExtractedTask> tasks;
    private List<ExtractedDecision> decisions;
    private List<ExtractedExperiment> experiments;
    private List<ExtractedResult> results;
    private List<ExtractedDeadline> deadlines;
    private List<ExtractedResearchEntity> researchEntities;
    private List<ExtractedResearchChange> researchChanges;

    public List<ExtractedTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<ExtractedTask> tasks) {
        this.tasks = tasks;
    }

    public List<ExtractedDecision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<ExtractedDecision> decisions) {
        this.decisions = decisions;
    }

    public List<ExtractedExperiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ExtractedExperiment> experiments) {
        this.experiments = experiments;
    }

    public List<ExtractedResult> getResults() {
        return results;
    }

    public void setResults(List<ExtractedResult> results) {
        this.results = results;
    }

    public List<ExtractedDeadline> getDeadlines() {
        return deadlines;
    }

    public void setDeadlines(List<ExtractedDeadline> deadlines) {
        this.deadlines = deadlines;
    }

    public List<ExtractedResearchEntity> getResearchEntities() {
        return researchEntities;
    }

    public void setResearchEntities(List<ExtractedResearchEntity> researchEntities) {
        this.researchEntities = researchEntities;
    }

    public List<ExtractedResearchChange> getResearchChanges() {
        return researchChanges;
    }

    public void setResearchChanges(List<ExtractedResearchChange> researchChanges) {
        this.researchChanges = researchChanges;
    }
}
