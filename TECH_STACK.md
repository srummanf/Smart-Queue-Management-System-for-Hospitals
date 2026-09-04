# Tech Stack & What We're Learning

## Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Language | Java 21 | Modern, stable, widely used in enterprise/backend jobs |
| Framework | Spring Boot 3.x | Standard for Java web apps, handles config/wiring for us |
| Build tool | Maven | Manages dependencies, builds the jar |
| Web layer | Spring MVC + Thymeleaf | Server renders HTML pages, no separate frontend build |
| Frontend | Plain HTML + a bit of vanilla JS | Only used for the WebSocket (STOMP) client, nothing more |
| Real-time updates | Spring WebSocket (STOMP/SockJS) | Pushes live queue changes to dashboard/patient pages, no page refresh |
| Database | PostgreSQL | Relational data (patients, tokens, doctors) fits a relational DB |
| Data access | Spring Data JPA | Write Java repository interfaces instead of raw SQL |
| DB migrations | Flyway | Versioned SQL scripts, so schema changes are tracked and repeatable |
| Security | Spring Security (form login) | Staff/doctor pages need login; patient pages stay open |
| Tests | JUnit 5 + Mockito | Standard Java testing tools |

**Deployment shape:** one Spring Boot jar + one Postgres database. No Docker,
no Redis, no Kafka, no microservices, no SPA framework. Everything runs as a
single app on a single machine — kept simple on purpose.

## What We're Learning

1. **Real-time web apps without complexity**
   Using WebSocket (STOMP) to push live updates to two different screens
   (doctor dashboard, patient tracking page) instead of the browser
   repeatedly asking the server "anything new?" (polling).

2. **One source of truth for business rules**
   The queue order (priority first, then who arrived earliest) is written
   in exactly one place in the code and reused everywhere it's needed. This
   avoids the bug where two screens show two different orderings because
   someone copy-pasted the logic.

3. **Getting useful data without a separate reporting system**
   Analytics (average wait time, patients per day) are calculated directly
   from the same `Token` table used to run the queue — no extra
   analytics/logging table. This teaches how to avoid building
   infrastructure you don't actually need yet.

4. **Simple, explainable predictions**
   Estimated wait time is a plain formula (average consultation time ×
   number of people ahead in queue) — not a machine learning model. This
   is easier to build, test, and explain to anyone reviewing the project.

5. **Mixed access rules in one app**
   Staff and doctors log in and have roles (`STAFF`, `DOCTOR`). Patients use
   a link with no login at all. Learning how to set up security rules so
   both cases work correctly in the same Spring Security config.

6. **Knowing when *not* to add complexity**
   The project intentionally skips patterns you'd see in bigger systems
   (interface/impl pairs, generic base classes, feature flags, Docker,
   microservices). The lesson here isn't the code — it's the judgment
   call of matching architecture to the actual size of the problem.
