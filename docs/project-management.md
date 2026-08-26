# Project Management Module

## Purpose

This module manages the basic structure of research projects in ResearchTrace.

It allows the professor to create and manage research projects, add students to projects, define project milestones, and store research supervision meetings associated with a project.

## Scope

This module is responsible for:

- Managing users (professors and students)
- Creating and managing research projects
- Adding students to projects
- Defining project milestones
- Storing project-related meetings

This module does not calculate student/project progress or maintain the research decision trace. Those are handled by the other project modules.

## Main Entities

### User

Represents a professor or student using the system.

### Project

Represents a research project being supervised.

### ProjectMember

Represents a student's participation in a particular project.

### Milestone

Represents a major stage or goal within a research project.

### Meeting

Represents a research supervision meeting associated with a project.

## Entity Relationships

### User and Project

A user can participate in multiple research projects, and a project can have multiple users.

The relationship is represented through the ProjectMember entity.

### ProjectMember

ProjectMember connects a student with a project.

This allows ResearchTrace to distinguish between:

- The overall progress of a project
- The individual contribution of each student

### Project and Milestone

A project can contain multiple milestones.

Each milestone belongs to exactly one project.

Example:

Project
→ Dataset Preparation
→ Baseline Evaluation
→ Model Evaluation
→ Final Analysis

### Project and Meeting

A project can have multiple research supervision meetings.

Each meeting belongs to one project.

Meeting information will later serve as input to the AI-assisted extraction module.

## Entity Fields

### User

- `id` — unique identifier
- `name` — user's name
- `email` — user's email address
- `role` — PROFESSOR or STUDENT

### Project

- `id` — unique identifier
- `name` — project name
- `description` — short description of the research project
- `status` — current project status
- `createdAt` — date/time when the project was created

### ProjectMember

- `id` — unique identifier
- `projectId` — project associated with the member
- `userId` — student associated with the project
- `role` — student's role in the project

### Milestone

- `id` — unique identifier
- `projectId` — project associated with the milestone
- `title` — milestone name
- `description` — description of the milestone
- `status` — NOT_STARTED, IN_PROGRESS, COMPLETED, BLOCKED, or DEFERRED
- `dueDate` — target completion date

### Meeting

- `id` — unique identifier
- `projectId` — project associated with the meeting
- `title` — meeting title
- `meetingDate` — date/time of the meeting
- `notes` — meeting notes or transcript
