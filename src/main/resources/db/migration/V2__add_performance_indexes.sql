-- Performance Optimization: Add Database Indexes
-- These indexes dramatically improve query performance for common lookups

-- User authentication queries (login)
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Study item queries by topic
CREATE INDEX IF NOT EXISTS idx_study_items_topic_id ON study_items(topic_id);

-- Topic queries by course
CREATE INDEX IF NOT EXISTS idx_topics_course_id ON topics(course_id);
CREATE INDEX IF NOT EXISTS idx_topics_course_order ON topics(course_id, order_index);

-- User progress queries
CREATE INDEX IF NOT EXISTS idx_user_progress_user_id ON user_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_user_progress_lookup ON user_progress(user_id, study_item_id);
CREATE INDEX IF NOT EXISTS idx_user_progress_next_review ON user_progress(user_id, next_review);

-- Study session queries
CREATE INDEX IF NOT EXISTS idx_study_sessions_user_id ON study_sessions(user_id);

-- Session log queries
CREATE INDEX IF NOT EXISTS idx_session_logs_session_id ON session_logs(session_id);
