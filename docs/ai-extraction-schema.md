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
