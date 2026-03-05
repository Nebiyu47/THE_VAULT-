-- src/main/resources/db/migration/V1__create_users_table.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email VARCHAR(255) UNIQUE NOT NULL,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100),
                       avatar_url TEXT,
                       role VARCHAR(20) DEFAULT 'BREACHER',
                       is_active BOOLEAN DEFAULT true,
                       is_verified BOOLEAN DEFAULT false,
                       vault_points INTEGER DEFAULT 1000,
                       total_wins INTEGER DEFAULT 0,
                       total_games INTEGER DEFAULT 0,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       last_login TIMESTAMP,
                       updated_at TIMESTAMP,
                       preferences JSONB,

                       CONSTRAINT username_length CHECK (char_length(username) >= 3),
                       CONSTRAINT valid_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
    );

-- Refresh tokens table
CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token VARCHAR(500) UNIQUE NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                revoked BOOLEAN DEFAULT false,
                                device_info JSONB,

                                INDEX idx_user_id (user_id),
                                INDEX idx_token (token)
);

-- User sessions table
CREATE TABLE user_sessions (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               session_token VARCHAR(500) UNIQUE NOT NULL,
                               ip_address INET,
                               user_agent TEXT,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               expires_at TIMESTAMP,
                               is_active BOOLEAN DEFAULT true,

                               INDEX idx_user_sessions (user_id),
                               INDEX idx_session_token (session_token)
);

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_vault_points ON users(vault_points DESC);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Insert default admin user (password: Admin@123)
INSERT INTO users (id, email, username, password_hash, full_name, role, is_verified)
VALUES (
           uuid_generate_v4(),
           'admin@thevault.com',
           'admin',
           '$2a$10$rJYqZ9Z9Z9Z9Z9Z9Z9Z9ZuZ9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9',
           'System Administrator',
           'ADMIN',
           true
       );