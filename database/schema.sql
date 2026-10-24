-- StudyHive reference/rebuild schema (MySQL 8).
--
-- This script is safe to re-run against the existing `studyhive` database:
-- every CREATE TABLE uses IF NOT EXISTS, so it will never drop or overwrite
-- tables that already exist. It documents the schema the backend expects.
--
-- Verified against the live database on 2026-10-19: all ten tables below
-- already existed with matching structure before any application code was
-- written. Only the `popular_materials` view was missing, so it is added
-- here.

CREATE DATABASE IF NOT EXISTS studyhive
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE studyhive;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    global_role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    account_status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS communities (
    community_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    community_type ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PUBLIC',
    join_code VARCHAR(20) NOT NULL UNIQUE,
    owner_id BIGINT UNSIGNED NOT NULL,
    private_password_hash VARCHAR(255),
    status ENUM('ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_community_owner
        FOREIGN KEY (owner_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS community_members (
    community_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    member_role ENUM('OWNER', 'MODERATOR', 'MEMBER')
        NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (community_id, user_id),
    CONSTRAINT fk_member_community
        FOREIGN KEY (community_id) REFERENCES communities(community_id),
    CONSTRAINT fk_member_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_community_members_user (user_id)
);

CREATE TABLE IF NOT EXISTS subjects (
    subject_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    community_id BIGINT UNSIGNED NOT NULL,
    subject_code VARCHAR(30) NOT NULL,
    subject_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (community_id, subject_code),
    CONSTRAINT fk_subject_community
        FOREIGN KEY (community_id) REFERENCES communities(community_id)
);

CREATE TABLE IF NOT EXISTS materials (
    material_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    community_id BIGINT UNSIGNED NULL,
    subject_id BIGINT UNSIGNED NULL,
    uploaded_by BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    material_type ENUM(
        'NOTE', 'TEXTBOOK', 'QUESTION_PAPER', 'PRESENTATION', 'OTHER'
    ) NOT NULL DEFAULT 'NOTE',
    book_name VARCHAR(200),
    author_name VARCHAR(150),
    visibility ENUM('COMMUNITY', 'PRIVATE') NOT NULL DEFAULT 'COMMUNITY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_material_community
        FOREIGN KEY (community_id) REFERENCES communities(community_id),
    CONSTRAINT fk_material_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id),
    CONSTRAINT fk_material_uploader
        FOREIGN KEY (uploaded_by) REFERENCES users(user_id),
    INDEX idx_material_community (community_id),
    INDEX idx_material_subject (subject_id),
    INDEX idx_material_uploader (uploaded_by),
    INDEX idx_material_title (title)
);

CREATE TABLE IF NOT EXISTS material_versions (
    version_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT UNSIGNED NOT NULL,
    version_number INT UNSIGNED NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_provider ENUM('LOCAL', 'CATALYST', 'FIREBASE', 'SUPABASE') NOT NULL DEFAULT 'LOCAL',
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(150),
    file_size_bytes BIGINT UNSIGNED NOT NULL,
    checksum_sha256 CHAR(64),
    uploaded_by BIGINT UNSIGNED NOT NULL,
    version_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (material_id, version_number),
    UNIQUE (storage_provider, storage_key),
    CONSTRAINT fk_version_material
        FOREIGN KEY (material_id) REFERENCES materials(material_id),
    CONSTRAINT fk_version_uploader
        FOREIGN KEY (uploaded_by) REFERENCES users(user_id),
    INDEX idx_version_material_number (material_id, version_number)
);

CREATE TABLE IF NOT EXISTS question_papers (
    question_paper_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT UNSIGNED NOT NULL UNIQUE,
    exam_year YEAR NULL,
    exam_type VARCHAR(100),
    semester TINYINT UNSIGNED NULL,
    CONSTRAINT chk_question_paper_semester
        CHECK (semester IS NULL OR semester BETWEEN 1 AND 12),
    CONSTRAINT fk_question_paper_material
        FOREIGN KEY (material_id) REFERENCES materials(material_id)
);

CREATE TABLE IF NOT EXISTS material_views (
    view_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_view_material
        FOREIGN KEY (material_id) REFERENCES materials(material_id),
    CONSTRAINT fk_view_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_material_view_count (material_id),
    INDEX idx_material_viewed_at (material_id, viewed_at)
);

CREATE TABLE IF NOT EXISTS material_reports (
    report_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT UNSIGNED NOT NULL,
    reported_by BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'REVIEWED', 'DISMISSED')
        NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_material
        FOREIGN KEY (material_id) REFERENCES materials(material_id),
    CONSTRAINT fk_report_user
        FOREIGN KEY (reported_by) REFERENCES users(user_id),
    INDEX idx_report_status_created (status, created_at)
);

CREATE TABLE IF NOT EXISTS material_favorites (
    user_id BIGINT UNSIGNED NOT NULL,
    material_id BIGINT UNSIGNED NOT NULL,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, material_id),
    CONSTRAINT fk_favorite_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_favorite_material
        FOREIGN KEY (material_id) REFERENCES materials(material_id)
);

CREATE OR REPLACE VIEW popular_materials AS
SELECT
    m.material_id,
    m.title,
    m.community_id,
    m.subject_id,
    COUNT(v.view_id) AS view_count
FROM materials m
LEFT JOIN material_views v ON v.material_id = m.material_id
GROUP BY
    m.material_id, m.title, m.community_id, m.subject_id;

-- Share-link (guest) read-only access; see migrations/003_add_community_guest_access.sql
CREATE TABLE IF NOT EXISTS community_guest_access (
    community_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (community_id, user_id),
    CONSTRAINT fk_guest_community
        FOREIGN KEY (community_id) REFERENCES communities(community_id) ON DELETE CASCADE,
    CONSTRAINT fk_guest_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_community_guest_access_user (user_id)
);
