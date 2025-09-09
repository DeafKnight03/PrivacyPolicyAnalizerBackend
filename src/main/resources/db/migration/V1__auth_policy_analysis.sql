-- ROLES & USERS
CREATE TABLE role (
  id   BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE app_user (
  id            BIGSERIAL PRIMARY KEY,
  username      TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role_id       BIGINT NOT NULL REFERENCES role(id),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- REFRESH TOKENS
CREATE TABLE refresh_token (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  token_hash TEXT   NOT NULL UNIQUE,
  issued_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked    BOOLEAN NOT NULL DEFAULT FALSE
);

-- DOMAIN: POLICY & ANALYSIS
CREATE TABLE policy (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  content     TEXT   NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE analysis (
  id          BIGSERIAL PRIMARY KEY,
  policy_id   BIGINT NOT NULL REFERENCES policy(id) ON DELETE CASCADE,
  data        JSONB  NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- INDEXES
CREATE INDEX idx_refresh_user_expires          ON refresh_token(user_id, expires_at);
CREATE INDEX idx_policy_user_created_at        ON policy(user_id, created_at);
CREATE INDEX idx_analysis_policy_created_at    ON analysis(policy_id, created_at);
-- (opzionale) Index GIN su JSONB se farai query sul contenuto
-- CREATE INDEX idx_analysis_data_gin ON analysis USING GIN (data);
