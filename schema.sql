-- Protocol: PostgreSQL
-- Create tables for VisiBoard Admin

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255),
    email VARCHAR(255),
    display_name VARCHAR(255),
    photo_url TEXT,
    is_banned BOOLEAN DEFAULT FALSE,
    ban_expiry BIGINT,
    is_restricted BOOLEAN DEFAULT FALSE,
    restriction_expiry BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notes (
    note_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES users(user_id),
    content TEXT,
    image_url TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    likes_count INT DEFAULT 0,
    is_hidden BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    liked_by_users TEXT[],
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reports (
    report_id VARCHAR(255) PRIMARY KEY,
    reporter_id VARCHAR(255), -- Not strictly enforcing FK if reporter is deleted, or sync order issues
    reported_user_id VARCHAR(255),
    reported_note_id VARCHAR(255), -- Nullable if reporting a user profile
    reason TEXT,
    target_details TEXT,
    type VARCHAR(50),
    timestamp BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, REVIEWED, DISMISSED, ACTION_TAKEN
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    type VARCHAR(50),
    message TEXT,
    timestamp BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
