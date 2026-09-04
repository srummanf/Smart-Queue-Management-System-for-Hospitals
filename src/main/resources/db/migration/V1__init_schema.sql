-- Baseline schema: departments, doctors, patients, tokens, staff users.
-- Queue ordering and wait-time logic live in application code (see PLAN.md §5);
-- this migration only defines structure + the constraints that protect it.

CREATE TABLE department (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE doctor (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL REFERENCES department (id)
);

CREATE TABLE patient (
    id        BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone     VARCHAR(20)  NOT NULL
);

CREATE TABLE token (
    id            BIGSERIAL PRIMARY KEY,
    token_number  INTEGER NOT NULL,
    patient_id    BIGINT NOT NULL REFERENCES patient (id),
    doctor_id     BIGINT NOT NULL REFERENCES doctor (id),
    priority      VARCHAR(10) NOT NULL DEFAULT 'NORMAL'
                    CHECK (priority IN ('NORMAL', 'PRIORITY', 'EMERGENCY')),
    status        VARCHAR(15) NOT NULL DEFAULT 'WAITING'
                    CHECK (status IN ('WAITING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    called_at     TIMESTAMP,
    completed_at  TIMESTAMP
);

-- Speeds up the one queue-ordering query every screen depends on
-- (WHERE doctor_id = ? AND status = 'WAITING' ORDER BY priority DESC, created_at ASC).
CREATE INDEX idx_token_doctor_status ON token (doctor_id, status);

CREATE TABLE staff_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(10) NOT NULL CHECK (role IN ('STAFF', 'DOCTOR')),
    doctor_id     BIGINT REFERENCES doctor (id)
);
