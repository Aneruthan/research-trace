package com.researchtrace.backend.dto.research;

import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.ResearchChange;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.ResearchEvent;
import com.researchtrace.backend.model.Result;

import java.util.ArrayList;
import java.util.List;

public class ResearchTraceSummary {

    private long totalDecisions;
    private long activeDecisions;
    private long totalExperiments;
    private long completedExperiments;
    private long totalResults;
    private long flaggedChanges;

    private List<ResearchDecision> decisions = new ArrayList<>();
    private List<Experiment> experiments = new ArrayList<>();
    private List<Result> results = new ArrayList<>();
    private List<ResearchEvent> timeline = new ArrayList<>();
    private List<ResearchChange> changes = new ArrayList<>();

    public long getTotalDecisions() {
        return totalDecisions;
    }

    public void setTotalDecisions(long totalDecisions) {
        this.totalDecisions = totalDecisions;
    }

    public long getActiveDecisions() {
        return activeDecisions;
    }

    public void setActiveDecisions(long activeDecisions) {
        this.activeDecisions = activeDecisions;
    }

    public long getTotalExperiments() {
        return totalExperiments;
    }

    public void setTotalExperiments(long totalExperiments) {
        this.totalExperiments = totalExperiments;
    }

    public long getCompletedExperiments() {
        return completedExperiments;
    }

    public void setCompletedExperiments(long completedExperiments) {
        this.completedExperiments = completedExperiments;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults;
    }

    public long getFlaggedChanges() {
        return flaggedChanges;
    }

    public void setFlaggedChanges(long flaggedChanges) {
        this.flaggedChanges = flaggedChanges;
    }

    public List<ResearchDecision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<ResearchDecision> decisions) {
        this.decisions = decisions;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<Experiment> experiments) {
        this.experiments = experiments;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public List<ResearchEvent> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<ResearchEvent> timeline) {
        this.timeline = timeline;
    }

    public List<ResearchChange> getChanges() {
        return changes;
    }

    public void setChanges(List<ResearchChange> changes) {
        this.changes = changes;
    }
}
