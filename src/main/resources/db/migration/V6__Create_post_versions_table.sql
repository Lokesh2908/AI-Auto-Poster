-- Create post_versions table for thread-based content refinement system
CREATE TABLE post_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    version_type VARCHAR(50) NOT NULL COMMENT 'refinement, full_regeneration, restore, backup_before_restore',
    change_description TEXT COMMENT 'Human-readable description of what changed',
    refinement_instructions TEXT COMMENT 'User instructions for refinements',
    previous_content TEXT COMMENT 'Content before this change',
    enhancement_options JSON COMMENT 'AI enhancement settings used',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    title VARCHAR(255) COMMENT 'Post title at this version',
    source_discussion TEXT COMMENT 'Source discussion at this version',
    target_platforms VARCHAR(100) COMMENT 'Target platforms at this version',
    
    -- Foreign key constraint
    CONSTRAINT fk_post_versions_post_id 
        FOREIGN KEY (post_id) REFERENCES posts(id) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- Indexes for performance
    INDEX idx_post_versions_post_id (post_id),
    INDEX idx_post_versions_created_at (created_at),
    INDEX idx_post_versions_version_type (version_type),
    INDEX idx_post_versions_post_created (post_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores version history for thread-based content refinement system';
