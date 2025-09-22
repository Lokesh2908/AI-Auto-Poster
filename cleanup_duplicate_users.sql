-- Script to identify and clean up duplicate users

-- 1. First, identify duplicate emails
SELECT email, COUNT(*) as count 
FROM users 
GROUP BY email 
HAVING COUNT(*) > 1;

-- 2. See all duplicate users with details
SELECT u1.id, u1.email, u1.first_name, u1.last_name, u1.is_active, u1.created_at
FROM users u1
WHERE u1.email IN (
    SELECT email 
    FROM users 
    GROUP BY email 
    HAVING COUNT(*) > 1
)
ORDER BY u1.email, u1.created_at;

-- 3. Keep only the oldest active user for each email (CAREFUL - BACKUP FIRST!)
-- This will delete newer duplicate users, keeping the first created active user
/*
DELETE u1 FROM users u1
INNER JOIN users u2 
WHERE u1.id > u2.id 
  AND u1.email = u2.email 
  AND u2.is_active = true;
*/

-- 4. Alternative: Keep the most recently created active user (if preferred)
/*
DELETE u1 FROM users u1
INNER JOIN users u2 
WHERE u1.id < u2.id 
  AND u1.email = u2.email 
  AND u2.is_active = true;
*/

-- 5. After cleanup, add unique constraint to prevent future duplicates
-- ALTER TABLE users ADD CONSTRAINT unique_email UNIQUE (email);
