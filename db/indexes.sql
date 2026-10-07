CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_job_status_posted   ON job (status, posted_at DESC);
CREATE INDEX idx_job_employment_type ON job (employment_type);
CREATE INDEX idx_job_max_salary      ON job (max_salary);
CREATE INDEX idx_job_skills_trgm     ON job USING gin (lower(required_skills) gin_trgm_ops);
CREATE INDEX idx_job_location_trgm   ON job USING gin (lower(location) gin_trgm_ops);