-- Insert initial admin user
INSERT INTO users (email, password, role, department, created_at, updated_at, is_active) 
VALUES ('admin@aiautoposter.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN', 'IT', NOW(), NOW(), true);

-- Insert sample manager user
INSERT INTO users (email, password, role, department, manager_id, created_at, updated_at, is_active) 
VALUES ('manager@aiautoposter.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'MANAGER', 'Marketing', 1, NOW(), NOW(), true);

-- Insert sample regular user
INSERT INTO users (email, password, role, department, manager_id, created_at, updated_at, is_active) 
VALUES ('user@aiautoposter.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', 'Marketing', 2, NOW(), NOW(), true);

-- Insert sample post
INSERT INTO posts (created_by, title, source_discussion, current_status, target_platforms, created_at, updated_at) 
VALUES (3, 'AI in Marketing', 'We discussed how AI is revolutionizing marketing strategies and customer engagement. The team shared insights about personalization, automation, and data-driven decision making.', 'DRAFT', 'linkedin', NOW(), NOW());

-- Insert sample post content

INSERT INTO post_content (post_id, platform, title, content, ai_confidence_score, hashtags, created_at, updated_at) 
VALUES (1, 'linkedin', 'AI in Marketing', '🚀 The Future of Marketing is Here: AI-Driven Strategies\n\nIn our recent team discussion, we explored how artificial intelligence is transforming the marketing landscape. From personalized customer experiences to automated campaign optimization, AI is enabling marketers to:\n\n• Deliver hyper-personalized content at scale\n• Predict customer behavior with unprecedented accuracy\n• Automate repetitive tasks and focus on strategy\n• Make data-driven decisions in real-time\n\nAs we continue to embrace these technologies, the question remains: How is your organization leveraging AI in marketing?\n\n#AIMarketing #DigitalTransformation #MarketingTech #Innovation #CustomerExperience', 0.85, '#AIMarketing #DigitalTransformation #MarketingTech #Innovation #CustomerExperience', NOW(), NOW());


-- Insert sample notification
INSERT INTO notifications (post_id, user_id, type, read_status, message, created_at, updated_at) 
VALUES (1, 2, 'APPROVAL_REQUEST', false, 'New post "AI in Marketing" requires your approval', NOW(), NOW());
