-- Insert Users
-- Password: admin123 (BCrypt encoded)
INSERT INTO users (username, password, role, enabled) 
SELECT 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJwC', 'ROLE_ADMIN', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password, role, enabled) 
SELECT 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJwC', 'ROLE_USER', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user');

-- Insert Accounts (linked to users)
INSERT INTO accounts (account_id, holder_name, balance, status, version, last_updated, user_id) 
SELECT 'ACC-001', 'John Doe', 10000.0000, 'ACTIVE', 0, CURRENT_TIMESTAMP, (SELECT id FROM users WHERE username = 'user' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE account_id = 'ACC-001');

INSERT INTO accounts (account_id, holder_name, balance, status, version, last_updated, user_id) 
SELECT 'ACC-002', 'Jane Smith', 5000.0000, 'ACTIVE', 0, CURRENT_TIMESTAMP, (SELECT id FROM users WHERE username = 'user' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE account_id = 'ACC-002');

INSERT INTO accounts (account_id, holder_name, balance, status, version, last_updated, user_id) 
SELECT 'ACC-003', 'Bob Johnson', 7500.0000, 'ACTIVE', 0, CURRENT_TIMESTAMP, (SELECT id FROM users WHERE username = 'user' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE account_id = 'ACC-003');

