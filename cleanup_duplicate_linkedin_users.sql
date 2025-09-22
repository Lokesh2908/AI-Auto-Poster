-- Script to identify and clean up duplicate LinkedIn users

-- 1. Check for duplicate LinkedIn users by user_id
SELECT user_id, COUNT(*) as count 
FROM linkedin_users 
GROUP BY user_id 
HAVING COUNT(*) > 1;

-- 2. Check for duplicate LinkedIn users by linkedin_user_id
SELECT linkedin_user_id, COUNT(*) as count 
FROM linkedin_users 
GROUP BY linkedin_user_id 
HAVING COUNT(*) > 1;

-- 3. See all duplicate LinkedIn users with details
SELECT lu.id, lu.user_id, lu.linkedin_user_id, lu.name, lu.email, lu.created_at
FROM linkedin_users lu
WHERE lu.user_id IN (
    SELECT user_id 
    FROM linkedin_users 
    GROUP BY user_id 
    HAVING COUNT(*) > 1
)
OR lu.linkedin_user_id IN (
    SELECT linkedin_user_id 
    FROM linkedin_users 
    GROUP BY linkedin_user_id 
    HAVING COUNT(*) > 1
)
ORDER BY lu.user_id, lu.created_at;

-- 4. Remove duplicate LinkedIn users (keep the oldest one for each user_id)
-- BACKUP YOUR DATABASE FIRST!
/*
DELETE lu1 FROM linkedin_users lu1
INNER JOIN linkedin_users lu2 
WHERE lu1.id > lu2.id 
  AND lu1.user_id = lu2.user_id;
*/

-- 5. Remove duplicate LinkedIn users by linkedin_user_id (keep the oldest one)
/*
DELETE lu1 FROM linkedin_users lu1
INNER JOIN linkedin_users lu2 
WHERE lu1.id > lu2.id 
  AND lu1.linkedin_user_id = lu2.linkedin_user_id;
*/
