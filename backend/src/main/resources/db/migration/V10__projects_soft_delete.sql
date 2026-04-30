-- T107: Add soft-delete support to projects table
ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_projects_deleted_at ON projects (deleted_at) WHERE deleted_at IS NULL;
