CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE relief_requests (
  id UUID PRIMARY KEY,
  reporter_id UUID NOT NULL REFERENCES users(id),
  need_type VARCHAR(20) NOT NULL,
  quantity INT NOT NULL CHECK (quantity > 0),
  urgency VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  place VARCHAR(255),
  description VARCHAR(2000) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_requests_status ON relief_requests(status);
CREATE INDEX idx_requests_urgency ON relief_requests(urgency);
CREATE INDEX idx_requests_reporter ON relief_requests(reporter_id);
CREATE INDEX idx_requests_coords ON relief_requests(lat, lng);

CREATE TABLE supply_points (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES users(id),
  name VARCHAR(150) NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE inventory_items (
  id UUID PRIMARY KEY,
  supply_point_id UUID NOT NULL REFERENCES supply_points(id) ON DELETE CASCADE,
  need_type VARCHAR(20) NOT NULL,
  quantity INT NOT NULL CHECK (quantity >= 0),
  UNIQUE (supply_point_id, need_type)
);

CREATE TABLE tasks (
  id UUID PRIMARY KEY,
  request_id UUID NOT NULL REFERENCES relief_requests(id),
  volunteer_id UUID REFERENCES users(id),
  status VARCHAR(20) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  accepted_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ
);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_volunteer ON tasks(volunteer_id);

CREATE TABLE audit_logs (
  id UUID PRIMARY KEY,
  actor_id UUID,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id UUID,
  details VARCHAR(1000),
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
