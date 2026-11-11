package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.ExtractedDecision;
import com.researchtrace.backend.dto.research.ExtractedExperiment;
import com.researchtrace.backend.dto.research.ExtractedResearchChange;
import com.researchtrace.backend.dto.research.ExtractedResearchEntity;
import com.researchtrace.backend.dto.research.ExtractedResult;
import com.researchtrace.backend.dto.research.ExtractedTask;
import com.researchtrace.backend.dto.research.MeetingExtractionResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic offline extractor for research notes when Gemini API is unavailable.
 * Provides predictable structured data for testing and demonstrations without network access.
 * Supports explicit line-prefixed records (DECISION:, EXPERIMENT:, RESULT:, CHANGE:, ENTITY:)
 * with graceful fallback to heuristic keyword extraction.
 */
public class StubResearchNotesExtractor implements ResearchNotesExtractor {

    private static final String PREFIX_DECISION = "DECISION:";
    private static final String PREFIX_EXPERIMENT = "EXPERIMENT:";
    private static final String PREFIX_RESULT = "RESULT:";
    private static final String PREFIX_CHANGE = "CHANGE:";
    private static final String PREFIX_ENTITY = "ENTITY:";

    @Override
    public MeetingExtractionResponse extract(String meetingNotes) {
        MeetingExtractionResponse response = new MeetingExtractionResponse();
        if (meetingNotes == null || meetingNotes.isBlank()) {
            response.setDecisions(new ArrayList<>());
            response.setExperiments(new ArrayList<>());
            response.setResults(new ArrayList<>());
            response.setResearchEntities(new ArrayList<>());
            response.setResearchChanges(new ArrayList<>());
            response.setDeadlines(new ArrayList<>());
            response.setTasks(new ArrayList<>());
            return response;
        }

        // Check if notes contain any explicit prefix lines
        if (hasExplicitPrefixes(meetingNotes)) {
            return extractExplicit(meetingNotes);
        }

        return extractHeuristic(meetingNotes);
    }

    private boolean hasExplicitPrefixes(String notes) {
        for (String rawLine : notes.split("\\R")) {
            String line = rawLine.trim().toUpperCase(Locale.ROOT);
            if (line.startsWith(PREFIX_DECISION) ||
                line.startsWith(PREFIX_EXPERIMENT) ||
                line.startsWith(PREFIX_RESULT) ||
                line.startsWith(PREFIX_CHANGE) ||
                line.startsWith(PREFIX_ENTITY)) {
                return true;
            }
        }
        return false;
    }

    private MeetingExtractionResponse extractExplicit(String notes) {
        MeetingExtractionResponse response = new MeetingExtractionResponse();
        List<ExtractedDecision> decisions = new ArrayList<>();
        List<ExtractedExperiment> experiments = new ArrayList<>();
        List<ExtractedResult> results = new ArrayList<>();
        List<ExtractedResearchEntity> entities = new ArrayList<>();
        List<ExtractedResearchChange> changes = new ArrayList<>();
        List<ExtractedTask> tasks = new ArrayList<>();

        for (String rawLine : notes.split("\\R")) {
            String line = rawLine.trim();
            String upper = line.toUpperCase(Locale.ROOT);

            if (upper.startsWith(PREFIX_DECISION)) {
                String body = line.substring(PREFIX_DECISION.length()).trim();
                String[] segments = splitSegments(body);
                if (segments.length > 0 && !segments[0].isBlank()) {
                    ExtractedDecision d = new ExtractedDecision();
                    d.setTitle(segments[0]);
                    d.setReason(segments.length > 1 ? segments[1] : "Supervisor consensus during meeting review.");
                    d.setDescription(segments.length > 2 ? segments[2] : segments[0]);
                    d.setMadeBy("Research Supervisor");
                    d.setConfidence(0.95);
                    decisions.add(d);
                }
            } else if (upper.startsWith(PREFIX_EXPERIMENT)) {
                String body = line.substring(PREFIX_EXPERIMENT.length()).trim();
                String[] segments = splitSegments(body);
                if (segments.length > 0 && !segments[0].isBlank()) {
                    ExtractedExperiment exp = new ExtractedExperiment();
                    exp.setTitle(segments[0]);
                    exp.setObjective(segments.length > 1 ? segments[1] : "Evaluate " + segments[0]);
                    exp.setVariables(segments.length > 2 ? segments[2] : "Experimental configuration and baseline comparison");
                    exp.setExpectedOutcome(segments.length > 3 ? segments[3] : "Statistically significant improvement over baseline");
                    exp.setDescription(segments.length > 1 ? segments[1] : segments[0]);
                    exp.setConfidence(0.95);
                    experiments.add(exp);
                }
            } else if (upper.startsWith(PREFIX_RESULT)) {
                String body = line.substring(PREFIX_RESULT.length()).trim();
                String[] segments = splitSegments(body);
                if (segments.length > 0 && !segments[0].isBlank()) {
                    ExtractedResult res = new ExtractedResult();
                    if (segments.length >= 3) {
                        res.setExperiment(segments[0]);
                        res.setMetrics(segments[1]);
                        res.setDescription(segments[2]);
                        res.setComparison(segments.length >= 4 ? segments[3] : "Consistent with empirical hypotheses");
                    } else if (segments.length == 2) {
                        // If segment 0 matches an experiment title, use it as experiment
                        String firstExp = !experiments.isEmpty() ? experiments.get(0).getTitle() : "Primary Experiment";
                        res.setExperiment(firstExp);
                        res.setMetrics(segments[0]);
                        res.setDescription(segments[1]);
                        res.setComparison("Consistent with empirical hypotheses");
                    } else {
                        String firstExp = !experiments.isEmpty() ? experiments.get(0).getTitle() : "Primary Experiment";
                        res.setExperiment(firstExp);
                        res.setMetrics(segments[0]);
                        res.setDescription(segments[0]);
                        res.setComparison("Consistent with empirical hypotheses");
                    }
                    res.setConfidence(0.95);
                    results.add(res);
                }
            } else if (upper.startsWith(PREFIX_CHANGE)) {
                String body = line.substring(PREFIX_CHANGE.length()).trim();
                String[] segments = splitSegments(body);
                if (segments.length > 0 && !segments[0].isBlank()) {
                    ExtractedResearchChange ch = new ExtractedResearchChange();
                    ch.setPreviousApproach(segments[0]);
                    ch.setNewApproach(segments.length > 1 ? segments[1] : "Updated methodology");
                    ch.setReason(segments.length > 2 ? segments[2] : "Methodological improvement identified in supervision.");
                    ch.setDescription(segments.length > 3 ? segments[3] : ("Change from " + segments[0] + " to " + (segments.length > 1 ? segments[1] : "updated approach")));
                    ch.setConfidence(0.95);
                    changes.add(ch);
                }
            } else if (upper.startsWith(PREFIX_ENTITY)) {
                String body = line.substring(PREFIX_ENTITY.length()).trim();
                String[] segments = splitSegments(body);
                if (segments.length > 0 && !segments[0].isBlank()) {
                    ExtractedResearchEntity entity = new ExtractedResearchEntity();
                    entity.setName(segments[0]);
                    entity.setType(segments.length > 1 ? segments[1].toUpperCase(Locale.ROOT) : "CONCEPT");
                    entity.setDescription(segments.length > 2 ? segments[2] : segments[0]);
                    entity.setConfidence(0.95);
                    entities.add(entity);
                }
            }
        }

        // If no explicit entities found, auto-detect entities from mentions
        if (entities.isEmpty()) {
            String lower = notes.toLowerCase(Locale.ROOT);
            if (lower.contains("tcga-brca") || lower.contains("dataset") || lower.contains("benchmark")) {
                ExtractedResearchEntity entity = new ExtractedResearchEntity();
                entity.setName("TCGA-BRCA");
                entity.setType("DATASET");
                entity.setDescription("TCGA Breast Invasive Carcinoma whole-slide pathology dataset");
                entity.setConfidence(0.95);
                entities.add(entity);
            }
            if (lower.contains("vit-b16") || lower.contains("vision transformer") || lower.contains("model")) {
                ExtractedResearchEntity entity = new ExtractedResearchEntity();
                entity.setName("ViT-B16");
                entity.setType("MODEL");
                entity.setDescription("Vision Transformer architecture for pathology patch classification");
                entity.setConfidence(0.95);
                entities.add(entity);
            }
        }

        response.setDecisions(decisions);
        response.setExperiments(experiments);
        response.setResults(results);
        response.setResearchEntities(entities);
        response.setResearchChanges(changes);
        response.setDeadlines(new ArrayList<>());
        response.setTasks(tasks);

        return response;
    }

    private MeetingExtractionResponse extractHeuristic(String meetingNotes) {
        MeetingExtractionResponse response = new MeetingExtractionResponse();
        String lower = meetingNotes.toLowerCase(Locale.ROOT);

        List<ExtractedDecision> decisions = new ArrayList<>();
        List<ExtractedExperiment> experiments = new ArrayList<>();
        List<ExtractedResult> results = new ArrayList<>();
        List<ExtractedResearchEntity> entities = new ArrayList<>();
        List<ExtractedResearchChange> changes = new ArrayList<>();
        List<ExtractedTask> tasks = new ArrayList<>();

        if (lower.contains("pmc-vqa") || lower.contains("benchmark") || lower.contains("dataset")) {
            ExtractedResearchEntity entity = new ExtractedResearchEntity();
            entity.setName("PMC-VQA");
            entity.setType("DATASET");
            entity.setDescription("Medical Visual Question Answering Benchmark Dataset");
            entity.setConfidence(0.95);
            entities.add(entity);
        }

        if (lower.contains("litmedimage")) {
            ExtractedResearchEntity entity = new ExtractedResearchEntity();
            entity.setName("LitMedImage");
            entity.setType("DATASET");
            entity.setDescription("Clinical image dataset with verified annotations");
            entity.setConfidence(0.95);
            entities.add(entity);
        }

        if (lower.contains("med-clip") || lower.contains("model")) {
            ExtractedResearchEntity entity = new ExtractedResearchEntity();
            entity.setName("Med-CLIP");
            entity.setType("MODEL");
            entity.setDescription("Domain-specific vision-language foundation model");
            entity.setConfidence(0.92);
            entities.add(entity);
        }

        // Detect Decisions
        if (lower.contains("decide") || lower.contains("agreed") || lower.contains("adopt") || lower.contains("switch") || lower.contains("use")) {
            ExtractedDecision d = new ExtractedDecision();
            if (lower.contains("switch") && lower.contains("litmedimage")) {
                d.setTitle("Switch dataset to LitMedImage");
                d.setDescription("Pivot from PMC-VQA to LitMedImage for primary evaluation.");
                d.setReason("PMC-VQA labels contained excessive annotation noise.");
            } else if (lower.contains("pmc-vqa")) {
                d.setTitle("Adopt PMC-VQA benchmark");
                d.setDescription("Use PMC-VQA as the evaluation benchmark for medical VQA.");
                d.setReason("Standard benchmark in current biomedical literature.");
            } else {
                d.setTitle("Adopt evaluation methodology");
                d.setDescription("Standardize evaluation protocol across student experiments.");
                d.setReason("Ensures consistent metric comparison.");
            }
            d.setMadeBy("Research Supervisor");
            d.setConfidence(0.90);
            decisions.add(d);
        }

        // Detect Experiments
        if (lower.contains("experiment") || lower.contains("evaluate") || lower.contains("test") || lower.contains("baseline") || lower.contains("ablation")) {
            ExtractedExperiment exp = new ExtractedExperiment();
            if (lower.contains("baseline")) {
                exp.setTitle("Baseline Zero-Shot Evaluation");
                exp.setDescription("Evaluate zero-shot accuracy of Med-CLIP on test split.");
                exp.setObjective("Establish zero-shot baseline performance.");
                exp.setVariables("Zero-shot prompt template standard");
                exp.setExpectedOutcome("Expected accuracy > 65%");
            } else {
                exp.setTitle("Model Ablation Study");
                exp.setDescription("Investigate influence of visual encoder fine-tuning.");
                exp.setObjective("Measure performance delta when visual encoder is frozen.");
                exp.setVariables("Frozen vs fine-tuned vision backbone");
                exp.setExpectedOutcome("Fine-tuning improves F1 by at least 5 points.");
            }
            exp.setConfidence(0.88);
            experiments.add(exp);
        }

        // Detect Results
        if (lower.contains("accuracy") || lower.contains("result") || lower.contains("achieved") || lower.contains("f1") || lower.contains("%")) {
            ExtractedResult res = new ExtractedResult();
            if (lower.contains("58.4") || lower.contains("below")) {
                res.setDescription("Zero-shot baseline reached 58.4% accuracy, below expectation.");
                res.setExperiment("Baseline Zero-Shot Evaluation");
                res.setMetrics("Accuracy: 58.4%, F1: 0.54");
                res.setComparison("Underperforms supervised baseline by 6.6%");
            } else {
                res.setDescription("Experimental run observed significant metric variation.");
                res.setExperiment("Model Ablation Study");
                res.setMetrics("Accuracy: 64.2%, F1: 0.61");
                res.setComparison("Consistent with literature expectations");
            }
            res.setConfidence(0.91);
            results.add(res);
        }

        // Detect Research Changes
        if (lower.contains("switch") || lower.contains("change") || lower.contains("pivot") || lower.contains("supersede") || lower.contains("instead of")) {
            ExtractedResearchChange ch = new ExtractedResearchChange();
            if (lower.contains("litmedimage")) {
                ch.setDescription("Dataset switch from PMC-VQA to LitMedImage");
                ch.setPreviousApproach("PMC-VQA benchmark dataset");
                ch.setNewApproach("LitMedImage dataset with verified annotations");
                ch.setReason("High annotation noise in PMC-VQA labels.");
            } else {
                ch.setDescription("Research Direction Adjustment");
                ch.setPreviousApproach("Initial proposed approach");
                ch.setNewApproach("Refined methodology based on experimental observations");
                ch.setReason("Initial experiments did not meet performance criteria.");
            }
            ch.setConfidence(0.89);
            changes.add(ch);
        }

        // Fallback default decision if none triggered
        if (decisions.isEmpty() && experiments.isEmpty() && results.isEmpty() && changes.isEmpty()) {
            ExtractedDecision defaultDecision = new ExtractedDecision();
            defaultDecision.setTitle("Proceed with proposed research methodology");
            defaultDecision.setDescription("Continue current experimental pipeline as discussed.");
            defaultDecision.setReason("Alignment with project milestones.");
            defaultDecision.setConfidence(0.80);
            decisions.add(defaultDecision);
        }

        response.setDecisions(decisions);
        response.setExperiments(experiments);
        response.setResults(results);
        response.setResearchEntities(entities);
        response.setResearchChanges(changes);
        response.setDeadlines(new ArrayList<>());
        response.setTasks(tasks);

        return response;
    }

    private String[] splitSegments(String body) {
        String[] parts = body.split("\\|", -1);
        String[] trimmed = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            trimmed[i] = parts[i].trim();
        }
        return trimmed;
    }
}
