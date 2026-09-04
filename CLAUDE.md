# CLAUDE.md — Working Agreement for This Project

Smart Queue Management System for Hospitals. See `PRD.md` for product scope
and `PLAN.md` for architecture/schema/build order — read both before making
structural decisions.

## Project Character (read this first)

This is a **minimalist resume/portfolio project**, not a production system.
The explicit goal is a small, clean, fully-understandable codebase that
demonstrates sound architecture — not feature breadth, scale, or robustness
beyond what's needed for a working demo.

When in doubt: **cut scope, don't add abstraction.** Concretely:
- No `service.impl` interface/impl pairing — concrete service classes only.
- No generic base repository/service classes, no mapper frameworks
  (MapStruct, ModelMapper) — write the two-line manual mapping instead.
- No speculative configurability (feature flags, strategy patterns) for
  requirements that don't exist yet.
- No Docker/Redis/Kafka/microservices — one Spring Boot jar, one Postgres DB.
- No SPA framework, no npm/webpack build — Thymeleaf + a little vanilla JS
  for the STOMP client.
- Prefer 3 similar lines over a premature shared helper.

If a task seems to call for more machinery than this, stop and ask rather
than building it — the PRD/PLAN may need to be updated first, or the request
may be out of the agreed scope.

## Tech Stack

Java 21, Spring Boot 3.x, Maven, Spring MVC + Thymeleaf, Spring Data JPA,
PostgreSQL, Flyway, Spring WebSocket (STOMP/SockJS), Spring Security
(form login), JUnit 5 + Mockito. Full rationale in `PLAN.md` §1.

## Architecture Rules

- **Queue ordering has exactly one definition**: `priority DESC, created_at
  ASC` over `WAITING` tokens for a doctor. Every place that needs queue
  order (dashboard, patient position, wait estimate) must derive from the
  same query/method — never re-implement the ordering logic in a second
  place.
- **No separate analytics/history table.** Historical stats and the
  wait-time estimator both read from completed `Token` rows. Don't introduce
  a `ConsultationRecord` or event-log table — see PLAN.md §4 for why.
- **Every state-changing `TokenService` operation must broadcast** via
  `QueueBroadcastService` (register, call-next, complete, cancel, escalate)
  so the doctor dashboard and patient portal stay live. If you add a new
  mutation, wire the broadcast — don't leave a silent state change.
- **Patient-facing routes are unauthenticated by design** (PRD §5) — don't
  add login requirements to `/patients/{tokenId}`. Staff/doctor routes
  require `STAFF`/`DOCTOR` roles.
- Wait-time estimation is a plain statistical formula (rolling average ×
  queue position) — do not introduce ML/prediction libraries for this.

## Commands

```bash
mvn spring-boot:run        # run the app
mvn test                   # run unit tests
mvn flyway:migrate         # apply DB migrations (also runs on app startup)
```

Postgres connection config lives in `src/main/resources/application.yml` —
expects a local Postgres instance; no Docker Compose is part of this project
(keep setup to "install Postgres, create DB, run the app").

## Testing Expectations

Unit-test the logic that's actually non-trivial: `TokenService` queue
ordering/state transitions and `WaitTimeEstimator` math (see PLAN.md §9).
Don't write tests for Thymeleaf templates or trivial entity getters/setters —
that's padding, not coverage, for a project this size.

## Documents in This Repo

- `PRD.md` — what to build and why (features, users, non-functional
  requirements, explicitly out-of-scope items).
- `PLAN.md` — how to build it (package layout, schema, endpoints, real-time
  design, phased build order).
- This file — standing rules for how to work in the codebase day to day.

Keep all three in sync: if a request changes scope (e.g., adds SMS
notifications, ML prediction, multi-tenancy), update `PRD.md`/`PLAN.md`
first rather than silently building it.
