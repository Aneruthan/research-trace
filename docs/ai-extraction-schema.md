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

### Deadline

A deadline represents a time constraint mentioned during a research meeting.

The AI should try to identify:

- `description` — what the deadline applies to
- `date` — specific date, if mentioned
- `relativeDate` — relative expression such as "by Friday" or "next week", if used
- `associatedTask` — task associated with the deadline, if identifiable
- `source` — meeting from which the deadline was extracted
- `confidence` — AI confidence in the extraction

### Research Entity

A research entity represents a named object or concept mentioned in a research meeting that may be relevant to the project's research context.

The AI should try to identify:

- `name` — name of the entity
- `type` — type of entity, such as MODEL, DATASET, PAPER, METHOD, TOOL, or METRIC
- `description` — additional information about the entity, if stated
- `context` — how the entity is being used or discussed in the meeting
- `source` — meeting from which the entity was extracted
- `confidence` — AI confidence in the extraction

### Possible Research Change

A possible research change represents an indication that the direction, method, experiment plan, or research priority may have changed during a meeting.

The AI should try to identify:

- `description` — what appears to have changed
- `previousApproach` — previous approach, method, experiment, or plan, if mentioned
- `newApproach` — new approach, method, experiment, or plan, if mentioned
- `reason` — stated reason for the change, if available
- `source` — meeting from which the change was extracted
- `confidence` — AI confidence in identifying the change

Possible changes should be presented to the user for review rather than being treated as confirmed research decisions automatically.

## User Review

AI-extracted information is considered a suggestion until reviewed by an authorized user.

The user should be able to:

- Accept an extracted item
- Edit an extracted item
- Reject an extracted item

The system should retain the original meeting as the source of the extracted information.

Research changes and decisions should not be added to the confirmed research trace solely based on AI extraction without user review.
