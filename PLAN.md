# PLAN — Technical Implementation Plan

Companion to PRD.md. This is the HOW: architecture, schema, endpoints, and
build order.

## 1. Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Web/View | Spring MVC + Thymeleaf (server-rendered) |
| Real-time | Spring WebSocket + STOMP over SockJS |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Auth | Spring Security (form login, roles `STAFF` / `DOCTOR`) |
| Testing | JUnit 5 + Mockito |
| Frontend assets | Plain CSS (or Bootstrap via WebJars) + a small amount of vanilla JS for the STOMP client — no SPA framework, no build tooling (no npm/webpack) |

No Docker/Redis/Kafka/microservices — single deployable Spring Boot jar
against one Postgres instance, matching the "minimalist" scope in the PRD.

## 2. Feature → Tech Stack Mapping

Mapped to the original 8 features from the product brief (see PRD.md §3),
in the same order/numbering:

1. **Patient registration and token generation**
   - Spring MVC controller + Thymeleaf form (staff-facing registration page)
   - `TokenService` (assigns token number, initial queue position)
   - Spring Data JPA — `Patient`, `Token` entities
   - PostgreSQL + Flyway migration for the schema

2. **Queue display for each doctor/department**
   - Spring Data JPA repository query: `ORDER BY priority DESC, created_at
     ASC` per doctor
   - Thymeleaf server-rendered view (`Department`/`Doctor` entities)

3. **Predict waiting time (historical data + real cases)**
   - Plain Java `WaitTimeEstimator` service — rolling average consultation
     time × queue position
   - JPA aggregate query over completed `Token` rows (no ML library —
     deliberate, see PRD §4)

4. **Live dashboard for doctors**
   - Spring WebSocket + STOMP over SockJS
   - `QueueBroadcastService` pushing to `/topic/doctor/{doctorId}/queue`
   - Thymeleaf page + small vanilla-JS STOMP client
   - Spring Security (`DOCTOR` role gate)

5. **Patient portal to track token status**
   - Same WebSocket/STOMP stack, topic `/topic/patient/{tokenId}`
   - Thymeleaf page at `/patients/{tokenId}` — unauthenticated route
   - Vanilla-JS STOMP client (no SPA framework)

6. **Emergency / priority patient handling**
   - `Token.priority` enum (`NORMAL`/`PRIORITY`/`EMERGENCY`) feeding the
     same ordering query as feature 2
   - Staff "escalate" endpoint (Spring MVC)
   - Re-broadcast via `QueueBroadcastService` so dashboards/portals reorder
     live

7. **Notifications when a patient's turn is near**
   - Same `/topic/patient/{tokenId}` STOMP topic, carrying a "turn near"
     flag once position/ETA crosses a threshold
   - In-app only — no SMS/email provider (Thymeleaf + JS shows an on-page
     banner)

8. **Historical analytics (daily load, avg consultation time)**
   - `AnalyticsService` — JPA aggregate queries over completed `Token` rows
   - Thymeleaf page (plain tables — no charting library, to stay minimal)
   - Spring Security (`STAFF` role gate)

**Cross-cutting (used by all 8):** Java 21, Spring Boot 3.x, Maven,
PostgreSQL, Flyway, Spring Security, JUnit 5 + Mockito for tests.

## 3. Package Structure

```
com.hospitalqueue
├── HospitalQueueApplication.java
├── config
│   ├── SecurityConfig.java
│   └── WebSocketConfig.java
├── entity
│   ├── Department.java
│   ├── Doctor.java
│   ├── Patient.java
│   ├── Token.java              (TokenStatus, Priority enums nested or top-level)
│   └── StaffUser.java          (login principal for STAFF/DOCTOR)
├── repository
│   ├── DepartmentRepository.java
│   ├── DoctorRepository.java
│   ├── PatientRepository.java
│   ├── TokenRepository.java
│   └── StaffUserRepository.java
├── service
│   ├── TokenService.java       (registration, queue ordering, state transitions)
│   ├── WaitTimeEstimator.java  (rolling avg + position -> ETA)
│   ├── QueueBroadcastService.java (pushes STOMP messages on any queue change)
│   └── AnalyticsService.java
├── controller
│   ├── PatientRegistrationController.java   (staff-facing form)
│   ├── QueueDashboardController.java        (doctor dashboard page)
│   ├── PatientStatusController.java         (/patients/{tokenId})
│   └── AnalyticsController.java
└── dto
    ├── TokenView.java          (what gets pushed over WebSocket / rendered)
    └── QueueSnapshot.java
```

No `service.impl` split, no MapStruct, no generic base repository/service —
concrete classes only, per the "no extra things" scope decision.

## 4. Data Model

```
Department(id, name)

Doctor(id, name, department_id -> Department)

Patient(id, full_name, phone)   -- minimal PII, no auth needed for patients

Token(
  id,
  token_number         -- sequential per doctor per day, shown to patient
  patient_id -> Patient
  doctor_id  -> Doctor
  priority             -- enum: NORMAL, PRIORITY, EMERGENCY
  status               -- enum: WAITING, IN_PROGRESS, COMPLETED, CANCELLED
  created_at
  called_at            -- set when doctor calls the patient (consultation start)
  completed_at         -- set when consultation ends
)

StaffUser(id, username, password_hash, role -- STAFF, DOCTOR, doctor_id nullable)
```

Deliberately **no separate `ConsultationRecord` / analytics table** —
historical analytics and the wait-time estimator both derive from completed
`Token` rows (`completed_at - called_at` = consultation duration,
`called_at - created_at` = actual wait). One source of truth, fewer moving
parts.

Indexes: `(doctor_id, status)` on `token` for fast queue lookups.

## 5. Queue Ordering & Wait-Time Algorithm

**Ordering** (per doctor, tokens with `status = WAITING`):
```
ORDER BY priority DESC (EMERGENCY, PRIORITY, NORMAL), created_at ASC
```
This is the single definition of "queue order" — used identically for the
dashboard list, the patient's position number, and the wait estimate. No
duplicated ordering logic.

**Wait-time estimate** for a given token:
```
avgConsultationMinutes = average of (completed_at - called_at) for that
                          doctor's last N completed tokens
                          (fallback default, e.g. 15 min, if no history yet)

estimatedWaitMinutes = position_ahead_in_queue * avgConsultationMinutes
```
Simple, explainable, no ML — matches the PRD's minimalist framing while still
satisfying "predict waiting time based on historical data."

**Priority handling**: registering a `PRIORITY`/`EMERGENCY` token doesn't move
any in-progress consultation; it only changes where the new token lands in
the `WAITING` ordering above. Escalating an existing waiting token's priority
is a simple status/priority update, re-evaluated by the same `ORDER BY`.

## 6. Real-Time Design (WebSocket/STOMP)

- One `QueueBroadcastService` method, called after every state-changing
  operation in `TokenService` (register, call-next, complete, cancel,
  escalate-priority).
- Topics:
  - `/topic/doctor/{doctorId}/queue` — full re-sorted queue snapshot for that
    doctor's dashboard.
  - `/topic/patient/{tokenId}` — that single patient's status, position, and
    ETA (also carries the "your turn is near" flag when position ≤
    threshold or ETA ≤ threshold).
- Client side: doctor dashboard and patient status page each open one STOMP
  connection and subscribe to their one relevant topic — no polling anywhere.
- Broadcasting the *whole* snapshot on change (not deltas) keeps client JS
  trivial (replace the DOM list) at the cost of a little extra payload —
  acceptable at this scale.

## 7. Key Routes

| Route | Who | Purpose |
|---|---|---|
| `GET/POST /register` | STAFF | Register patient, pick department/doctor/priority, issue token |
| `GET /doctors/{doctorId}/dashboard` | DOCTOR | Live queue view + "call next" / "complete" actions |
| `POST /doctors/{doctorId}/call-next` | DOCTOR | Move next WAITING token to IN_PROGRESS |
| `POST /tokens/{tokenId}/complete` | DOCTOR | Mark IN_PROGRESS token COMPLETED |
| `POST /tokens/{tokenId}/escalate` | STAFF | Bump priority of a waiting token |
| `GET /patients/{tokenId}` | Patient (no auth) | Live status/position/ETA page |
| `GET /analytics` | STAFF | Daily load + avg consultation time charts/tables |
| `/ws` | — | STOMP endpoint (SockJS handshake) |

## 8. Build Order (incremental phases, each independently demoable)

1. **Project skeleton** — Spring Boot init, Postgres connection, Flyway
   baseline migration, health check page.
2. **Core domain** — `Department`, `Doctor`, `Patient`, `Token` entities +
   repositories + migrations; seed a couple of departments/doctors.
3. **Registration + token generation** — staff form → `TokenService.register`
   → token number assignment.
4. **Queue display (no real-time yet)** — doctor dashboard as a plain
   Thymeleaf page reflecting the `ORDER BY` query, manual refresh.
5. **Wait-time estimator** — `WaitTimeEstimator`, shown on dashboard + a
   placeholder patient page.
6. **WebSocket wiring** — `WebSocketConfig`, `QueueBroadcastService`, convert
   dashboard to live-updating.
7. **Patient portal** — `/patients/{tokenId}` live page, "turn is near" alert.
8. **Emergency/priority handling** — registration priority field +
   escalate endpoint, verify ordering.
9. **Historical analytics** — `AnalyticsService` queries + simple page.
10. **Auth** — Spring Security form login for STAFF/DOCTOR, lock down
    staff/doctor routes; patient routes stay open.
11. **Tests + polish** — unit tests for `TokenService` ordering and
    `WaitTimeEstimator`; README with setup/run instructions and a short
    architecture note (good for resume/interview walkthrough).

## 9. Testing Strategy

Focus tests where the logic is non-trivial and demo-relevant:
- `TokenService`: registering into correct position, priority ordering
  correctness, call-next/complete state transitions, cancel.
- `WaitTimeEstimator`: rolling average calculation, fallback default,
  position-based ETA math.
- Skip testing Thymeleaf views and trivial CRUD getters/setters.
