package com.researchtrace.backend.dto.research;

import com.researchtrace.backend.model.Experiment;
import com.researchtrace.backend.model.Meeting;
import com.researchtrace.backend.model.ResearchDecision;
import com.researchtrace.backend.model.Result;

import java.util.ArrayList;
import java.util.List;

public class DecisionLineageView {

    private ResearchDecision decision;
    private ResearchDecision predecessorDecision;
    private ResearchDecision successorDecision;
    private Meeting sourceMeeting;
    private List<ExperimentEvidence> experiments = new ArrayList<>();

    public static class ExperimentEvidence {
        private Experiment experiment;
        private List<Result> results = new ArrayList<>();

        public ExperimentEvidence() {
        }

        public ExperimentEvidence(Experiment experiment, List<Result> results) {
            this.experiment = experiment;
            this.results = results != null ? results : new ArrayList<>();
        }

        public Experiment getExperiment() {
            return experiment;
        }

        public void setExperiment(Experiment experiment) {
            this.experiment = experiment;
        }

        public List<Result> getResults() {
            return results;
        }

        public void setResults(List<Result> results) {
            this.results = results;
        }
    }

    public ResearchDecision getDecision() {
        return decision;
    }

    public void setDecision(ResearchDecision decision) {
        this.decision = decision;
    }

    public ResearchDecision getPredecessorDecision() {
        return predecessorDecision;
    }

    public void setPredecessorDecision(ResearchDecision predecessorDecision) {
        this.predecessorDecision = predecessorDecision;
    }

    public ResearchDecision getSuccessorDecision() {
        return successorDecision;
    }

    public void setSuccessorDecision(ResearchDecision successorDecision) {
        this.successorDecision = successorDecision;
    }

    public Meeting getSourceMeeting() {
        return sourceMeeting;
    }

    public void setSourceMeeting(Meeting sourceMeeting) {
        this.sourceMeeting = sourceMeeting;
    }

    public List<ExperimentEvidence> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ExperimentEvidence> experiments) {
        this.experiments = experiments;
    }
}
