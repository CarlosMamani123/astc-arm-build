-- Roles
INSERT INTO roles (id, code, name) VALUES ('018f6b7a-0001-7000-8000-000000000001', 'ADMIN', 'Administrator');
INSERT INTO roles (id, code, name) VALUES ('018f6b7a-0001-7000-8000-000000000002', 'PROJECT_MANAGER', 'Project Manager');
INSERT INTO roles (id, code, name) VALUES ('018f6b7a-0001-7000-8000-000000000003', 'TEAM_MEMBER', 'Team Member');

-- Projects
INSERT INTO projects (id, code, name, status) VALUES ('018f6b7a-0002-7000-8000-000000000001', 'PROJ-001', 'Main Platform', 'ACTIVE');
INSERT INTO projects (id, code, name, status) VALUES ('018f6b7a-0002-7000-8000-000000000002', 'PROJ-002', 'Mobile App', 'ACTIVE');

-- Admin user (password: Admin123!)
INSERT INTO users (id, username, first_name, last_name, full_name, email, password_hash, phone, role_id, project_id, status, created_at)
VALUES ('018f6b7a-0003-7000-8000-000000000001', 'admin', 'System', 'Admin', 'System Admin',
        'admin@backoffice.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '+1234567890', '018f6b7a-0001-7000-8000-000000000001', '018f6b7a-0002-7000-8000-000000000001',
        'ACTIVE', NOW());
