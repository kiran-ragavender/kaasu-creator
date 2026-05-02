-- Kaasu Creator — PostgreSQL schema (Supabase)
-- BIGSERIAL replaces MySQL's AUTO_INCREMENT

CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS expenses (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    category    VARCHAR(100) NOT NULL,
    amount      DECIMAL(10,2) NOT NULL,
    date        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jobs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_name    VARCHAR(255)  NOT NULL,
    hourly_wage DECIMAL(10,2) NOT NULL,
    notes       TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS timesheet_entries (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id       BIGINT        REFERENCES jobs(id) ON DELETE SET NULL,
    work_date    DATE          NOT NULL,
    hours_worked DECIMAL(10,2) NOT NULL,
    notes        TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS incomes (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    income_type  VARCHAR(20)  NOT NULL DEFAULT 'EXTRA',
    source       VARCHAR(255) NOT NULL,
    category     VARCHAR(100),
    amount       DECIMAL(10,2) NOT NULL,
    income_date  DATE,
    notes        TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS goals (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name           VARCHAR(255)  NOT NULL,
    target_amount  DECIMAL(10,2) NOT NULL,
    current_amount DECIMAL(10,2) DEFAULT 0,
    deadline       DATE          NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS roadmaps (
    id            BIGSERIAL PRIMARY KEY,
    goal_id       BIGINT        NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    week_number   INT           NOT NULL,
    target_amount DECIMAL(10,2) NOT NULL,
    status        VARCHAR(50)   DEFAULT 'pending'
);

CREATE INDEX IF NOT EXISTS idx_expenses_user_id        ON expenses(user_id);
CREATE INDEX IF NOT EXISTS idx_goals_user_id           ON goals(user_id);
CREATE INDEX IF NOT EXISTS idx_incomes_user_id         ON incomes(user_id);
CREATE INDEX IF NOT EXISTS idx_jobs_user_id            ON jobs(user_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_user_id       ON timesheet_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_job_id        ON timesheet_entries(job_id);
CREATE INDEX IF NOT EXISTS idx_roadmaps_goal_id        ON roadmaps(goal_id);
