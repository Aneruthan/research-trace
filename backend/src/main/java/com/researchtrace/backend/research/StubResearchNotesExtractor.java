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

/**
 * Deterministic offline extractor for research notes when Gemini API is unavailable.
 * Provides predictable structured data for testing and demonstrations without network access.
 */
public class StubResearchNotesExtractor implements ResearchNotesExtractor {

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

        String lower = meetingNotes.toLowerCase();

        List<ExtractedDecision> decisions = new ArrayList<>();
        List<ExtractedExperiment> experiments = new ArrayList<>();
        List<ResultHolder> resultHolders = new ArrayList<>();
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

    private static class ResultHolder {}
}
