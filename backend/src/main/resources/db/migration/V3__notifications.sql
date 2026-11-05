CREATE TABLE notifications (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  channel VARCHAR(20) NOT NULL,
  subject VARCHAR(200) NOT NULL,
  message VARCHAR(1000) NOT NULL,
  delivered BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_notifications_user ON notifications(user_id);
