-- Additive integrity migration for an existing PostgreSQL database.
-- It does not drop tables, delete rows, or rewrite history.
-- Foreign keys are added NOT VALID so existing orphan rows are left in place.
-- New writes are checked. Run the VALIDATE statements at the bottom only after
-- you have inspected any orphans. VALIDATE fails without deleting data.
--
-- Apply manually, for example:
--   psql "$DB_URL" -f docker/initdb/02-integrity.sql
-- This file is not executed by the application or by the test suite.

ALTER TABLE public.extraction_review ADD COLUMN IF NOT EXISTS original_extraction_json text;
ALTER TABLE public.experiment ADD COLUMN IF NOT EXISTS extraction_review_id bigint;
ALTER TABLE public.result ADD COLUMN IF NOT EXISTS extraction_review_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.users
        WHERE email IS NOT NULL AND btrim(email) <> ''
        GROUP BY lower(email)
        HAVING count(*) > 1
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower ON public.users (lower(email));
    ELSE
        RAISE NOTICE 'Skipped uk_users_email_lower because duplicate emails exist. No rows were changed.';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.project_member
        WHERE project_id IS NOT NULL AND user_id IS NOT NULL
        GROUP BY project_id, user_id
        HAVING count(*) > 1
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_project_member_project_user
            ON public.project_member (project_id, user_id);
    ELSE
        RAISE NOTICE 'Skipped uk_project_member_project_user because duplicate memberships exist. No rows were changed.';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_project_member_project ON public.project_member (project_id);
CREATE INDEX IF NOT EXISTS idx_meeting_project ON public.meeting (project_id);
CREATE INDEX IF NOT EXISTS idx_task_project ON public.task (project_id);
CREATE INDEX IF NOT EXISTS idx_milestone_project ON public.milestone (project_id);
CREATE INDEX IF NOT EXISTS idx_research_decision_project ON public.research_decision (project_id);
CREATE INDEX IF NOT EXISTS idx_experiment_project ON public.experiment (project_id);
CREATE INDEX IF NOT EXISTS idx_result_project ON public.result (project_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_supervisor') THEN
        ALTER TABLE public.project
            ADD CONSTRAINT fk_project_supervisor FOREIGN KEY (supervisor_id) REFERENCES public.users (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_member_project') THEN
        ALTER TABLE public.project_member
            ADD CONSTRAINT fk_project_member_project FOREIGN KEY (project_id) REFERENCES public.project (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_member_user') THEN
        ALTER TABLE public.project_member
            ADD CONSTRAINT fk_project_member_user FOREIGN KEY (user_id) REFERENCES public.users (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_meeting_project') THEN
        ALTER TABLE public.meeting
            ADD CONSTRAINT fk_meeting_project FOREIGN KEY (project_id) REFERENCES public.project (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_milestone_project') THEN
        ALTER TABLE public.milestone
            ADD CONSTRAINT fk_milestone_project FOREIGN KEY (project_id) REFERENCES public.project (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_project') THEN
        ALTER TABLE public.task
            ADD CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES public.project (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_meeting') THEN
        ALTER TABLE public.task
            ADD CONSTRAINT fk_task_meeting FOREIGN KEY (meeting_id) REFERENCES public.meeting (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_milestone') THEN
        ALTER TABLE public.task
            ADD CONSTRAINT fk_task_milestone FOREIGN KEY (milestone_id) REFERENCES public.milestone (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_assignee') THEN
        ALTER TABLE public.task
            ADD CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES public.users (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_extraction_review_meeting') THEN
        ALTER TABLE public.extraction_review
            ADD CONSTRAINT fk_extraction_review_meeting FOREIGN KEY (meeting_id) REFERENCES public.meeting (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_research_decision_project') THEN
        ALTER TABLE public.research_decision
            ADD CONSTRAINT fk_research_decision_project FOREIGN KEY (project_id) REFERENCES public.project (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_experiment_project') THEN
        ALTER TABLE public.experiment
            ADD CONSTRAINT fk_experiment_project FOREIGN KEY (project_id) REFERENCES public.project (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_result_project') THEN
        ALTER TABLE public.result
            ADD CONSTRAINT fk_result_project FOREIGN KEY (project_id) REFERENCES public.project (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_result_experiment') THEN
        ALTER TABLE public.result
            ADD CONSTRAINT fk_result_experiment FOREIGN KEY (experiment_id) REFERENCES public.experiment (id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_history_task') THEN
        ALTER TABLE public.task_history
            ADD CONSTRAINT fk_task_history_task FOREIGN KEY (task_id) REFERENCES public.task (id) NOT VALID;
    END IF;
END $$;

-- Optional, after inspecting orphans. These statements do not delete rows.
-- ALTER TABLE public.meeting VALIDATE CONSTRAINT fk_meeting_project;
-- ALTER TABLE public.task VALIDATE CONSTRAINT fk_task_project;
-- ALTER TABLE public.task VALIDATE CONSTRAINT fk_task_milestone;
-- ALTER TABLE public.project_member VALIDATE CONSTRAINT fk_project_member_project;
-- ALTER TABLE public.project_member VALIDATE CONSTRAINT fk_project_member_user;
