-- Additive migration: share-link (guest) access to communities.
-- A logged-in user who opens a community's share link (and gives the
-- community password, if it has one) gets read-only access to its
-- materials, subjects and question papers WITHOUT becoming a member.
-- Uploading, adding subjects and seeing the member list stay members-only.
--
-- Run once on every database the backend uses (local AND Aiven), e.g.:
--   mysql -u root -p studyhive < database/migrations/003_add_community_guest_access.sql
-- The backend uses ddl-auto=validate, so it will refuse to start until
-- this table exists.

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
