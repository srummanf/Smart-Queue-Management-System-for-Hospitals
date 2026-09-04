# Demo Login Credentials

This is an academic/portfolio project — these are seeded demo accounts only
(see `V3__seed_staff_users.sql`), not production credentials. Anyone running
the project locally can use them to log in at `/login`.

| Username | Password    | Role   | Doctor          |
|----------|-------------|--------|-----------------|
| staff    | staff123    | STAFF  | —               |
| simi     | simi123     | DOCTOR | Dr. Simi        |
| noor     | noor123     | DOCTOR | Dr. Noor        |
| shyam    | shyam123    | DOCTOR | Dr. Shyam       |
| george   | george123   | DOCTOR | Dr. George      |

- **STAFF** can register patients (`/register`), escalate priority, and view
  analytics (`/analytics`).
- **DOCTOR** can view their queue dashboard (`/doctors/{id}/dashboard`) and
  call next / complete consultations.
- `/patients/{tokenId}` (patient status tracking) needs no login by design.
