-- Drop media URL columns from study_items table
-- Media import functionality has been removed from the application

ALTER TABLE study_items DROP COLUMN IF EXISTS image_url;
ALTER TABLE study_items DROP COLUMN IF EXISTS audio_url;
