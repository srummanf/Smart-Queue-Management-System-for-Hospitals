# PRD — Smart Queue Management System for Hospitals

## 1. Summary

A web application that lets hospital staff register patients and issue tokens,
shows each doctor/department a live queue, predicts patient waiting time from
historical consultation data, and lets a patient watch their own token status
in real time — including emergency/priority handling and "your turn is near"
alerts.

**Project context:** simple, minimalist resume/portfolio project. Optimize for
a clean, small, understandable codebase that still demonstrates sound
architecture — not for scale, HA, or feature breadth. When in doubt, cut
scope rather than add complexity.

## 2. Users

- **Receptionist / staff** — registers walk-in patients, generates tokens,
  marks emergency/priority cases.
- **Doctor** — views their live queue on a dashboard, calls the next patient,
  marks a consultation complete.
- **Patient** — has no login; opens a link to `/patients/{tokenId}` to watch
  their own position and estimated wait time update live.
- **Admin** (optional, may collapse into staff role) — views historical
  analytics (daily load, average consultation time per doctor/department).

## 3. Core Features (from brief)

1. **Patient registration and token generation**
   - Staff registers a patient (name, phone, department, doctor) and the
     system issues a token with a queue position.
2. **Queue display for each doctor/department**
   - A view listing all waiting tokens for a doctor, ordered by priority then
     arrival time.
3. **Predicted waiting time**
   - Estimated wait computed from the doctor's historical average
     consultation time and the patient's position in queue. No ML — a
     transparent, explainable statistical estimate (see PLAN.md).
4. **Live dashboard for doctors**
   - Doctor's queue view updates in real time (WebSocket) as patients are
     registered, called, or completed — no manual refresh.
5. **Patient portal to track token status**
   - `/patients/{tokenId}` page shows current status (waiting / in progress /
     completed), live position, and live estimated wait — pushed over
     WebSocket, no polling, no login.
6. **Emergency / priority patient handling**
   - A token can be flagged `PRIORITY` or `EMERGENCY` at registration (or
     escalated later), which reorders it ahead of `NORMAL` tokens without
     discarding fairness among peers of the same priority (FIFO within a
     priority tier).
7. **Notifications when a patient's turn is near**
   - In-app only (no SMS/email). When a patient's token reaches a configurable
     threshold (e.g., 2nd in line, or ≤10 min estimated wait), the patient
     portal page shows a visible "you're next soon" alert, pushed live.
8. **Historical analytics**
   - A simple analytics view: patients served per day, average consultation
     time per doctor/department, average wait time per day. Backed by
     querying completed tokens — no separate reporting pipeline.

## 4. Out of Scope

- SMS/email/push notifications, or any third-party notification provider.
- Machine-learning based wait-time prediction (explicitly a simple
  statistical average instead).
- Multi-hospital / multi-tenant support.
- Payments, insurance, EHR/medical-record integration.
- Native mobile apps.
- Horizontal scaling, caching layers, message queues — single Spring Boot
  instance is the target deployment.
- Fine-grained audit logging / compliance (HIPAA-grade controls) — this is a
  resume project, not a real clinical deployment.

## 5. Non-Functional Requirements

- **Simplicity first**: prefer one obvious way to do something over
  configurability. A reviewer should be able to read the codebase in one
  sitting.
- **Correctness of queue ordering** matters more than raw performance —
  queue state must never be ambiguous (e.g., two patients believing they're
  "next").
- **Real-time feel**: dashboard and patient portal must reflect a queue
  change within ~1 second via WebSocket push, not polling.
- **Basic security**: staff/doctor actions (registration, calling patients)
  require login; the patient status page requires no login (token ID acts as
  a shareable, low-sensitivity link — no PII exposed beyond first name).
- **Testability**: core logic (queue ordering, wait-time estimation) covered
  by unit tests, since it's the most reviewable/demoable part of the code.

## 6. Success Criteria

- A demo flow works end-to-end: staff registers 3+ patients across
  priorities → doctor dashboard reorders live → patient portal for each
  token shows live position/ETA updates → completing a token in the
  dashboard instantly updates everyone watching → analytics page shows
  today's stats.
- Codebase is small enough to explain fully in a resume/interview
  conversation (layered architecture, WebSocket usage, queue algorithm).

## 7. Open Questions / Assumptions Made

These were decided to keep the project minimal; flag if you want them
changed:

- **Auth**: simple Spring Security form login for staff/doctors only
  (roles: `STAFF`, `DOCTOR`). Patient portal is unauthenticated.
- **Wait-time formula**: `positionInQueue × doctor's rolling average
  consultation time`, no ML.
- **Priority algorithm**: strict priority tiers (`EMERGENCY` > `PRIORITY` >
  `NORMAL`), FIFO within a tier.
- **Single hospital, multiple departments, multiple doctors per department.**
- **Build tool**: Maven.
