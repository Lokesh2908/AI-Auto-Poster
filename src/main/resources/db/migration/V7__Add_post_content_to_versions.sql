-- Add post content fields to post_versions table for proper restore functionality
-- This enables storing the actual AI-generated content that users see

ALTER TABLE post_versions 
ADD COLUMN saved_content LONGTEXT COMMENT 'JSON array of PostContent objects saved at this version',
ADD COLUMN content_summary TEXT COMMENT 'Brief summary of content for display purposes';

-- Create index for performance
CREATE INDEX idx_post_versions_content_summary ON post_versions(content_summary(100));

-- Add comment to explain the new functionality
ALTER TABLE post_versions 
COMMENT = 'Stores version history including actual post content for thread-based content refinement system';
