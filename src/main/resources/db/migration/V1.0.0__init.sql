-- V1.0.0__init.sql

CREATE TABLE profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    picture TEXT,
    job_title TEXT,
    bio TEXT,
    city TEXT,
    country TEXT,
    phone TEXT,
    email TEXT,
    site TEXT
);

CREATE TABLE bio_section (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE bio_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    section_id INTEGER NOT NULL,
    header TEXT,
    title TEXT,
    link TEXT,
    content TEXT,
    logo_label TEXT,
    logo_image_url TEXT,
    logo_link TEXT,
    collapsible INTEGER DEFAULT 0,
    collapsed INTEGER DEFAULT 0,
    ruler INTEGER DEFAULT 0,
    parent_id INTEGER,
    sort_order INTEGER DEFAULT 0,
    FOREIGN KEY (section_id) REFERENCES bio_section(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES bio_item(id) ON DELETE CASCADE
);

CREATE TABLE social_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0
);
