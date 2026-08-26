# AI Meeting Extraction Schema

## Purpose

This document defines the structured information that the AI component of ResearchTrace should extract from research meeting notes or transcripts.

The extracted information will be reviewed by the user before it is used to update the project's tasks, research history, or progress information.

## Input

The AI component receives:

- Meeting title
- Meeting date
- Project associated with the meeting
- Meeting notes or transcript

## Extraction Categories

The AI should identify the following categories of information:

- Tasks
- Decisions
- Experiments
- Results
- Deadlines
- Research entities
- Possible changes in research direction

## Extracted Information Structures

### Task

A task represents an action that needs to be performed as part of the research project.

The AI should try to identify:

- `title` — what needs to be done
- `description` — additional details about the task
- `assignee` — student responsible for the task, if explicitly identifiable
- `deadline` — deadline mentioned in the meeting, if available
- `source` — meeting from which the task was extracted
- `confidence` — AI confidence in the extraction

### Decision

A decision represents a research-related choice or conclusion made during a meeting.

The AI should try to identify:

- `title` — short description of the decision
- `description` — details explaining what was decided
- `madeBy` — person or people associated with the decision, if explicitly identifiable
- `reason` — stated reasoning or evidence behind the decision, if available
- `source` — meeting from which the decision was extracted
- `confidence` — AI confidence in the extraction

### Experiment

An experiment represents a research investigation or evaluation that is planned, discussed, or conducted during a meeting.

The AI should try to identify:

- `title` — short description of the experiment
- `description` — details of what will be tested or evaluated
- `objective` — what the experiment is intended to investigate, if stated
- `variables` — models, datasets, configurations, or other factors involved, if identifiable
- `expectedOutcome` — expected result or comparison, if stated
- `source` — meeting from which the experiment was extracted
- `confidence` — AI confidence in the extraction

### Result

A result represents an outcome or observation from an experiment that is discussed in the meeting.

The AI should try to identify:

- `description` — what was observed or measured
- `experiment` — experiment associated with the result, if identifiable
- `metrics` — measurements, scores, or quantitative values mentioned
- `comparison` — comparison with a baseline, previous result, or another experiment, if stated
- `source` — meeting from which the result was extracted
- `confidence` — AI confidence in the extraction
