-- Add NEEDS_UPDATE to applications.status and optional update_reason for admin notes.

ALTER TABLE applications DROP CONSTRAINT IF EXISTS applications_status_check;

ALTER TABLE applications
    ADD CONSTRAINT applications_status_check
    CHECK (status IN ('NEW', 'IN_REVIEW', 'NEEDS_UPDATE', 'APPROVED', 'REJECTED', 'CANCELLED'));

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS update_reason VARCHAR(1000);
