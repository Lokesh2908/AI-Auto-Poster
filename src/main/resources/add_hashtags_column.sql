-- Add hashtags column to post_content table (if it doesn't exist)
-- This is safe to run multiple times
ALTER TABLE post_content ADD COLUMN IF NOT EXISTS hashtags TEXT;

-- Update existing post content with extracted hashtags from their content
UPDATE post_content 
SET hashtags = CASE 
    WHEN content LIKE '%#AIMarketing%' THEN '#AIMarketing #DigitalTransformation #MarketingTech #Innovation #CustomerExperience'
    WHEN content LIKE '%#%' THEN SUBSTRING(content FROM '#[A-Za-z0-9_]+')
    ELSE ''
END
WHERE hashtags IS NULL;
