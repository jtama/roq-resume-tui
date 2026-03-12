CREATE TABLE resume (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    slug TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO resume (slug, title)
SELECT 'default',
       COALESCE(
           NULLIF(TRIM(COALESCE((SELECT first_name FROM profile LIMIT 1), '') || ' ' || COALESCE((SELECT last_name FROM profile LIMIT 1), '')), ''),
           'Default CV'
       )
WHERE EXISTS (SELECT 1 FROM profile)
  AND NOT EXISTS (SELECT 1 FROM resume);

ALTER TABLE profile ADD COLUMN resume_id INTEGER;
UPDATE profile
SET resume_id = (SELECT id FROM resume WHERE slug = 'default')
WHERE resume_id IS NULL;

ALTER TABLE social_item ADD COLUMN resume_id INTEGER;
UPDATE social_item
SET resume_id = (SELECT id FROM resume WHERE slug = 'default')
WHERE resume_id IS NULL;

ALTER TABLE bio_section ADD COLUMN resume_id INTEGER;
UPDATE bio_section
SET resume_id = (SELECT id FROM resume WHERE slug = 'default')
WHERE resume_id IS NULL;

CREATE INDEX idx_profile_resume_id ON profile(resume_id);
CREATE INDEX idx_social_item_resume_id ON social_item(resume_id);
CREATE INDEX idx_bio_section_resume_id ON bio_section(resume_id);
