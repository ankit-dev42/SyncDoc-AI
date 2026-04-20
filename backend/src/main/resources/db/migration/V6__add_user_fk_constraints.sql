-- V6__add_user_fk_constraints.sql
-- Adds foreign key constraints from user_subscriptions and projects to the users table.
-- Also aligns column types to UUID to match users.id (previously VARCHAR(100)).
-- Safe for empty dev/test databases; include USING cast for PostgreSQL type coercion.

ALTER TABLE user_subscriptions
    ALTER COLUMN user_id TYPE UUID USING user_id::UUID;

ALTER TABLE projects
    ALTER COLUMN owner_id TYPE UUID USING owner_id::UUID;

ALTER TABLE user_subscriptions
    ADD CONSTRAINT fk_user_subscriptions_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE projects
    ADD CONSTRAINT fk_projects_owner
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
