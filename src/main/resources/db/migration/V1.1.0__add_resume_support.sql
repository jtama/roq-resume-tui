-- V1.1.0__add_resume_support.sql

CREATE TABLE resume (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL
);

-- Insert a default resume to migrate existing data
INSERT INTO resume (id, name) VALUES (1, 'Default');

-- Update profile
CREATE TABLE profile_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resume_id INTEGER NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    picture TEXT,
    job_title TEXT,
    bio TEXT,
    city TEXT,
    country TEXT,
    phone TEXT,
    email TEXT,
    site TEXT,
    FOREIGN KEY (resume_id) REFERENCES resume(id) ON DELETE CASCADE
);
INSERT INTO profile_new (id, resume_id, first_name, last_name, picture, job_title, bio, city, country, phone, email, site)
SELECT id, 1, first_name, last_name, picture, job_title, bio, city, country, phone, email, site FROM profile;
DROP TABLE profile;
ALTER TABLE profile_new RENAME TO profile;

-- Update bio_section
CREATE TABLE bio_section_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resume_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    FOREIGN KEY (resume_id) REFERENCES resume(id) ON DELETE CASCADE
);
INSERT INTO bio_section_new (id, resume_id, title, sort_order)
SELECT id, 1, title, sort_order FROM bio_section;
DROP TABLE bio_section;
ALTER TABLE bio_section_new RENAME TO bio_section;

-- Update social_item
CREATE TABLE social_item_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resume_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    FOREIGN KEY (resume_id) REFERENCES resume(id) ON DELETE CASCADE
);
INSERT INTO social_item_new (id, resume_id, name, url, sort_order)
SELECT id, 1, name, url, sort_order FROM social_item;
DROP TABLE social_item;
ALTER TABLE social_item_new RENAME TO social_item;
