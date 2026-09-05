# ExcelMail Pro — Development Milestone 2

## Objective

Convert the refactored ExcelMail Pro **core from Milestone 1** into a structured Spring Boot SaaS backend.

This repository is already a Maven Spring Boot **CLI** (`com.mailSender`, `WebApplicationType.NONE`). Milestone 2 adds the backend foundation required for:

- User accounts
- Organizations
- Contact lists and contacts
- Email templates
- SMTP configurations
- Campaigns and campaign recipients
- Campaign execution (background worker)
- Usage tracking
- REST APIs under `/api/v1/`
- PostgreSQL + Flyway
- Authentication foundation
- API documentation
- Automated tests (no live SMTP)

At the end of this milestone, the project should have a working Spring Boot backend that can be tested through REST APIs, while the Excel → template → `EmailSender` pipeline from Milestone 1 still works (CLI and/or worker).

Read [`AGENTS.md`](AGENTS.md) first. Milestone 1 history lives in [`DEVELOPMENT_TASKS.md`](DEVELOPMENT_TASKS.md). Handoff notes: [`docs/NEXT_MILESTONE.md`](docs/NEXT_MILESTONE.md).

---

# 1. Milestone 2 Scope

## IN SCOPE

- Spring Boot **web** backend (in addition to, not instead of, the existing core)
- REST API
- PostgreSQL
- Flyway
- JPA/Hibernate
- Database entities, repositories, services, controllers, DTOs
- Validation and global exception handling
- Authentication and authorization
- Organization / user relationship
- Contact list management
- Excel/CSV upload API (reuse `ExcelReader` for `.xlsx`; CSV is new)
- Contact import
- Email template management (reuse `template.*`)
- SMTP configuration management (reuse `SmtpConfiguration` / `EmailSender`; encrypt passwords at rest)
- SMTP test connection (safe errors; no credential leakage)
- Campaign creation, lifecycle, recipients
- Background campaign processing foundation
- Sending status tracking
- Usage tracking foundation (not billing)
- OpenAPI documentation
- Unit, integration, and tenant-isolation tests
- Security hardening (no secrets in git)

---

# 2. OUT OF SCOPE

DO NOT implement the following during Milestone 2:

- React / Next.js frontend
- Payment gateway (Razorpay, Stripe)
- Subscription billing
- License generation / desktop licensing
- Production deployment, Kubernetes, Docker Swarm
- AI email generation
- CRM, WhatsApp, LinkedIn automation
- Open tracking, click tracking, email tracking pixels
- Marketing automation
- SMTP-limit bypassing
- Spam-bypass functionality
- Kafka or Redis unless a later ID explicitly requires them (none do)

These belong in later milestones.

---

# 3. Agent Operating Rules

## Rule 1 — Read Milestone 1 first

Before making changes:

1. Read `DEVELOPMENT_TASKS.md` (Milestone 1 record).
2. Read `README.md`.
3. Inspect the repository (`com.mailSender`: excel, template, campaign, smtp, config, `BatchMailRunner`, `MailBody`).
4. Understand Excel processing, templates, and the SMTP abstraction.
5. Run `mvn -q test`.

Do not assume Milestone 1 matches a generic greenfield plan. This app is Java 17+, Maven, CLI batch with `mail.batch-enabled` / `mail.dry-run`.

---

# 4. Rule 2 — Preserve existing functionality

The existing Excel → template → SMTP functionality must continue to work.

Prefer:

```text
Existing Core
     ↓
Spring Boot Service Layer
     ↓
REST API
```

Do not replace `ExcelReader`, `TemplateRenderer`, `EmailComposer`, or `EmailSender` without a documented reason. Keep dry-run available. Startup must not send mail when the API process is running (`mail.batch-enabled` stays false by default).

---

# Compatibility contract — do not change Milestone 1 behavior

Milestone 2 **adds** a SaaS API. It must **not** replace or silently alter the working CLI product. After every M2 ID, `mvn -q test` must still pass, including existing CLI / Excel / template / SMTP tests.

## Two run modes (required)

| Mode | How operators run it | Must keep |
| --- | --- | --- |
| **CLI** (default today) | `mvn spring-boot:run` without an API profile | `WebApplicationType.NONE`, no HTTP port, `BatchMailRunner` as today, **no PostgreSQL required**, process finishes after the runner (skip / batch / test-send) |
| **API** | Explicit profile (e.g. `api` / `saas`) | Servlet, `/api/v1/*`, Postgres + Flyway, auth, worker **does not** send on HTTP threads |

Do **not** globally change `MailSenderApplication` to servlet-only. Do **not** make DataSource/Flyway/Security mandatory for CLI or for existing `@SpringBootTest(webEnvironment = NONE)` tests.

Spring Security, JPA, and Flyway must be profile-gated or auto-config excluded so `mvn -q test` does not need a running Postgres.

The campaign **worker must not run** in CLI mode (would send or poll on `spring-boot:run`).

## Frozen CLI / core behavior

Do not change these unless a later milestone explicitly says so:

| Area | Frozen behavior |
| --- | --- |
| Defaults | `mail.batch-enabled=false`, `mail.dry-run=true`, `mail.test-send-enabled=false` |
| `mail.*` / `spring.mail.*` keys | Keep names, env vars, and defaults (including Gmail-oriented host/port and default subject). Add new keys; do not rename or invert defaults. |
| Test-send | `mail.test-send-enabled` + `mail.test-send-to` still sends **one** message, not the Excel list |
| Sent log | CLI still uses `SentAddressLog` / `mail.sent-log-path`. API campaigns use `campaign_recipients`; do not delete the file log. |
| Attachments | `mail.attachment-path` / `MailBodyAttachment` still work on CLI. Missing file still fails before the loop on real send. |
| HTML | `mail.html` still controls MIME |
| Delay | `mail.send-delay-ms` between real sends; not in dry-run; not after last |
| Fail isolation | One SMTP error does not abort remaining recipients; summary sent/failed/skipped; non-zero exit if any send failed |
| Fail loud | Missing/unreadable Excel or body file is an error, not empty success |
| Templates | UTF-8; `{{placeholders}}` case-insensitive; validate against Excel columns |
| Excel CLI | `.xlsx` only via `ExcelReader`; header detection; trim; skip blank/`@`-invalid; keep-first duplicates; `DataFormatter` |
| Email validity | Still `contains("@")` in the shared validator unless a dedicated ID documents a change **and** CLI tests are updated |
| Dry-run | Logs To + body; does not call `JavaMailSender`; does not append sent-log |
| Password masking | `SmtpConfiguration.toString()` stays `password=***` |
| Profiles | `development` / `production` remain; both keep batch off / dry-run on unless overridden |
| Class names | Do not rename `MailBody` as a drive-by. Do not move packages in a way that breaks `ArchitectureLayeringTest` without updating that test **and** keeping excel → template → campaign → smtp one-way. |
| Spring Boot / Java | Stay on Boot 3.2.x / Java 17 unless a task explicitly upgrades |

## How to add CSV / APIs without changing Excel

- Implement CSV as a **new** importer used by the upload API.
- Do **not** change `ExcelReader` row rules to “support CSV.”
- API import summary may wrap `ExcelReadResult` counts; do not drop invalid-row reporting.

## Existing tests are a gate

Every M2 ID must keep these green (extend, do not delete without replacement):

- `ExcelReaderTest`, `ExcelValidatorTest`, template tests
- `MailBody*` / `BatchMailRunner*` / `SentAddressLogTest`
- `SmtpEmailSender*` (mocked `JavaMailSender`)
- `@SpringBootTest` config/profile tests with `WebEnvironment.NONE` and batch off
- `ArchitectureLayeringTest`
- `OriginalExcelSmtpRegressionTest` / e2e local workflow tests (still no live SMTP)

If a change would require Postgres for those tests, the change is wrong: gate JPA behind the API profile or use Testcontainers **only** for new M2 integration tests.

---

# 5. Rule 3 — Inspect before creating classes

Before creating an entity, repository, service, controller, configuration, or utility, check whether an equivalent already exists. Avoid duplicate Excel parsers or SMTP senders.

---

# 6. Rule 4 — Database migrations are mandatory

Do not create tables only in JPA `ddl-auto=update` for the real schema.

All schema changes go through Flyway:

```text
src/main/resources/db/migration/
```

Naming:

```text
V1__create_users.sql
V2__create_organizations.sql
V3__create_contact_lists.sql
```

Never modify an already-applied migration. Never put secrets in SQL.

---

# 7. Rule 5 — Never expose secrets

Never:

- Commit SMTP passwords, JWT secrets, encryption keys, or database passwords
- Log SMTP passwords
- Return SMTP passwords from APIs
- Store user passwords as plaintext
- Hardcode JWT or encryption secrets

Use environment variables. Keep `application-local.properties` and `.env` gitignored.

---

# 8. Rule 6 — Use DTOs

Do not expose JPA entities through REST APIs.

---

# 9. Rule 7 — Tenant isolation

Every organization must only access its own contacts, lists, templates, SMTP configs, campaigns, recipients, and usage.

Never allow access by guessing another organization’s ID in the URL.

---

# 10. Task Status

Use:

```text
[ ] Not Started
[~] In Progress
[x] Completed
[!] Blocked
```

Mark `[~]` before starting. Mark `[x]` only after implementation, `mvn -q test`, verification without live SMTP, and documentation. If blocked, `[!]` plus reason.

---

# PHASE 1 — Project Assessment

## M2-001 — Inspect Milestone 1 implementation [x]

Inspect:

- Project structure (`com.mailSender`)
- Core classes (`MailBody`, `BatchMailRunner`, `MailSenderApplication`)
- Excel, template, SMTP, config modules
- Existing tests and `mail.*` keys

Document:

```text
Current Architecture
Reusable Components
Components Requiring Adaptation
Components Requiring Replacement
Potential Risks
```

Create/update:

```text
docs/MILESTONE2_ANALYSIS.md
```

---

## M2-002 — Verify baseline [x]

Run:

```text
mvn -q test
```

Record results in `docs/MILESTONE2_ANALYSIS.md` (or a short baseline subsection).

Milestone 2 must not proceed if the project is fundamentally broken unless the blocker is documented as `[!]`.

Do not send real mail. Do not require `MAIL_PASSWORD`.

---

# PHASE 2 — Spring Boot Foundation

## M2-003 — Establish Spring Boot web application [~]

The project is already Spring Boot CLI. **Add** web support; do not convert the only entrypoint to an always-on servlet.

- Add `spring-boot-starter-web` in a way that the **API profile** uses servlet.
- **CLI / default** stays `WebApplicationType.NONE` (no Tomcat on `mvn spring-boot:run` as operators use it today).
- Keep CLI batch behind `mail.batch-enabled` (default false).
- API process: `BatchMailRunner` must not send; worker must not start a campaign on boot.
- Existing `@SpringBootTest(webEnvironment = NONE)` must still start **without** Postgres, Flyway, or Security blocking the context.
- Do not blindly upgrade Spring Boot or Java.
- Add Spring Web, Data JPA, Security, Validation, PostgreSQL driver, Flyway, Actuator **when the matching ID needs them**, gated so CLI tests stay green.

Java stays **17+** (`pom.xml` `java.version` 17, Boot **3.2.3**) unless a later task explicitly upgrades it.

---

## M2-004 — Define package structure [ ]

Recommended under `com.mailSender`:

```text
com.mailSender
│
├── auth
├── user
├── organization
├── contact
├── template          (existing)
├── smtp              (existing)
├── campaign          (existing + persistence)
├── usage
├── excel             (existing)
├── common
│   ├── exception
│   ├── security
│   ├── response
│   └── validation
└── config            (existing)
```

Adapt; do not create a second root package. Avoid unnecessary complexity.

---

# PHASE 3 — PostgreSQL + Flyway

## M2-005 — Configure PostgreSQL [ ]

Configure URL, username, password, driver, connection pool via environment variables, for example:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

Do not commit real values. Document keys in README and `application-local.properties.example`.

CLI and existing context tests must still run **without** `DB_PASSWORD` or a live Postgres (exclude DataSource auto-config outside the API profile, or equivalent).

Tightly coupled with **M2-006**.

---

## M2-006 — Configure Flyway [ ]

Enable Flyway. Verify (against a local or test database, not production):

```text
Application startup
Database connection
Migration execution
```

Tightly coupled with **M2-005**.

---

## M2-007 — Create users table [ ]

Migration for `users`:

```text
id
email
password_hash
name
status
email_verified
created_at
updated_at
```

Requirements: unique email, indexes, timestamps, constraints.

---

# PHASE 4 — Organization / Tenant Model

## M2-008 — Create organizations table [ ]

```text
id
name
owner_id
status
created_at
updated_at
```

---

## M2-009 — Create organization membership [ ]

Table `organization_members`:

```text
id
organization_id
user_id
role
created_at
```

Roles: `OWNER`, `ADMIN`, `MEMBER`. Do not over-engineer permissions yet.

---

## M2-010 — Implement organization isolation [ ]

Organization-owned resources must contain or resolve to `organization_id`. Services verify membership before access.

---

# PHASE 5 — Authentication

## M2-011 — Implement registration [ ]

```http
POST /api/v1/auth/register
```

```json
{
  "name": "Peter",
  "email": "user@example.com",
  "password": "********"
}
```

Requirements: validate email and password, reject duplicates, hash password, create user, create organization, create owner membership. No plaintext passwords.

---

## M2-012 — Implement login [ ]

```http
POST /api/v1/auth/login
```

Return an authenticated token/session. Do not expose password information.

---

## M2-013 — Implement authentication security [ ]

Implement password hashing, authentication mechanism, authorization, secure token handling, configurable secret, expiration, and basic rate limiting where appropriate. Document decisions in `docs/SECURITY.md` when that file exists (or in the analysis doc until M2-069).

---

## M2-014 — Implement current-user endpoint [ ]

```http
GET /api/v1/auth/me
```

Return safe user information. Never return `password_hash`, SMTP password, or security secrets.

---

# PHASE 6 — Contact Lists

## M2-015 — Create contact_lists table [ ]

```text
id
organization_id
name
source_filename
total_contacts
created_at
updated_at
```

Tightly coupled with **M2-016**.

---

## M2-016 — Create contacts table [ ]

```text
id
contact_list_id
organization_id
email
name
status
metadata_json
created_at
updated_at
```

Must support arbitrary Excel columns (store extras in `metadata_json`). Align placeholder keys with Milestone 1 (`{{name}}`, `{{email}}`, extra columns lowercased). Duplicate-email policy: keep first unless product docs change it.

Tightly coupled with **M2-015**.

---

## M2-017 — Contact list API [ ]

```http
POST   /api/v1/contact-lists
GET    /api/v1/contact-lists
GET    /api/v1/contact-lists/{id}
DELETE /api/v1/contact-lists/{id}
```

Paginate list endpoints.

---

# PHASE 7 — Excel / CSV Import API

## M2-018 — Upload contact list [ ]

```http
POST /api/v1/contact-lists/{id}/upload
```

Support `.xlsx` (reuse `ExcelReader` **as-is**) and `.csv` (new parser, do not fold CSV into `ExcelReader`). Validate file type and size. Do not put POI in controllers. Do not change CLI `.xlsx` rules (headers, duplicates, `@` check).

---

## M2-019 — Import validation [ ]

Return a summary (same spirit as Milestone 1 logs):

```json
{
  "totalRows": 500,
  "valid": 472,
  "invalid": 18,
  "duplicates": 10
}
```

Do not silently discard invalid rows.

---

## M2-020 — Import errors [ ]

Identify invalid rows without dumping unnecessary personal data:

```text
Row 17: Invalid email format
Row 31: Missing email
```

---

# PHASE 8 — Email Templates

## M2-021 — Create email_templates table [ ]

```text
id
organization_id
name
subject
body
created_at
updated_at
```

---

## M2-022 — Template API [ ]

```http
POST   /api/v1/templates
GET    /api/v1/templates
GET    /api/v1/templates/{id}
PUT    /api/v1/templates/{id}
DELETE /api/v1/templates/{id}
```

---

## M2-023 — Template validation [ ]

Support `{{Name}}`, `{{Email}}`, `{{Company}}` (case-insensitive, Milestone 1 rules). Reuse `TemplateValidator` / `TemplateRenderer`. Validate placeholders against imported contact columns when a list is selected.

---

# PHASE 9 — SMTP Configuration

## M2-024 — Create SMTP accounts table [ ]

```text
id
organization_id
provider
host
port
username
encrypted_password
from_email
from_name
tls_enabled
created_at
updated_at
```

Tightly coupled with **M2-025**.

---

## M2-025 — Encrypt SMTP credentials [ ]

Do not store SMTP passwords as plaintext. Use encryption key + key version + ciphertext. Key from environment. Map decrypted values into existing `SmtpConfiguration` at send time only.

Tightly coupled with **M2-024**.

---

## M2-026 — SMTP API [ ]

```http
POST   /api/v1/smtp
GET    /api/v1/smtp
GET    /api/v1/smtp/{id}
DELETE /api/v1/smtp/{id}
POST   /api/v1/smtp/{id}/test
```

Never return the SMTP password.

---

## M2-027 — SMTP test [ ]

Validate configuration, attempt connection/authentication, return success/failure with a safe error. Do not expose credentials. Automated tests must mock the connection; do not use real App Passwords in CI.

---

# PHASE 10 — Campaign Model

## M2-028 — Create campaigns table [ ]

```text
id
organization_id
name
contact_list_id
template_id
smtp_account_id
status
total_recipients
queued_count
sent_count
failed_count
created_at
started_at
completed_at
updated_at
```

Statuses: `DRAFT`, `READY`, `RUNNING`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELLED`.

Tightly coupled with **M2-029**.

---

## M2-029 — Create campaign_recipients table [ ]

```text
id
campaign_id
contact_id
email
status
attempt_count
last_error
queued_at
sent_at
updated_at
```

Statuses: `PENDING`, `PROCESSING`, `SENT`, `FAILED`, `SKIPPED`.

This table replaces file-backed `SentAddressLog` for API campaigns.

Tightly coupled with **M2-028**.

---

## M2-030 — Campaign creation API [ ]

```http
POST /api/v1/campaigns
```

Validate that contact list, template, and SMTP account exist, belong to the organization, and that the name is valid.

---

# PHASE 11 — Campaign Lifecycle

## M2-031 — Campaign details [ ]

```http
GET /api/v1/campaigns
GET /api/v1/campaigns/{id}
```

Paginate lists.

---

## M2-032 — Start campaign [ ]

```http
POST /api/v1/campaigns/{id}/start
```

Validate campaign, recipients, SMTP, template. Transition `READY → RUNNING`. Do not send the whole list on the HTTP thread.

---

## M2-033 — Pause campaign [ ]

```http
POST /api/v1/campaigns/{id}/pause
```

Allowed: `RUNNING → PAUSED`.

---

## M2-034 — Resume campaign [ ]

```http
POST /api/v1/campaigns/{id}/resume
```

Allowed: `PAUSED → RUNNING`.

---

## M2-035 — Cancel campaign [ ]

```http
POST /api/v1/campaigns/{id}/cancel
```

Prevent new recipients from being processed after cancellation.

---

# PHASE 12 — Background Email Processing

## M2-036 — Design campaign worker [ ]

Safe background processing. Spring Scheduler or a simple poller is enough. Do **not** add Kafka for complexity. Worker should call `EmailComposer` + `EmailSender`. Honor `mail.dry-run` / per-tenant equivalent in non-prod.

Disable the worker in CLI mode. Do not send from the HTTP request thread. Keep per-recipient failure isolation and `mail.send-delay-ms` semantics for real sends.

---

## M2-037 — Recipient processing [ ]

```text
Campaign
    ↓
Pending Recipient
    ↓
Load Contact
    ↓
Render Template
    ↓
Create EmailMessage
    ↓
EmailSender
    ↓
SMTP
    ↓
Update Recipient Status
```

Reuse Milestone 1 compose/send types.

---

## M2-038 — Sending status [ ]

Success: `PENDING → PROCESSING → SENT`. Failure: `PENDING → PROCESSING → FAILED`. Record safe failure information (classifier already exists in `SmtpFailureClassifier`).

---

## M2-039 — Retry mechanism [ ]

Limited, configurable retries. Do not retry permanent failures indefinitely.

---

## M2-040 — Prevent duplicate sends [ ]

Protect against HTTP retries, restart, worker failure, and transaction retry. Use state transitions / locking / idempotency.

---

# PHASE 13 — Sending Controls

## M2-041 — Configurable sending delay [ ]

Configurable delay between sends/batches (map from existing `mail.send-delay-ms`). Do not hardcode.

---

## M2-042 — Provider-aware limits [ ]

Configuration for maximum recipients per campaign, maximum sends per interval, maximum daily usage. Do **not** bypass provider limits.

---

## M2-043 — Campaign safety validation [ ]

Before launch: recipient count, SMTP availability, template validity, required placeholders, campaign status, usage limits.

---

# PHASE 14 — Usage Tracking

## M2-044 — Create usage_records table [ ]

```text
id
organization_id
date
emails_attempted
emails_sent
emails_failed
created_at
updated_at
```

---

## M2-045 — Usage service [ ]

Track emails attempted/sent/failed, campaign count, contact count. Add a tenant-scoped read API (for example `GET /api/v1/usage`) so OpenAPI can document usage. Do **not** implement billing.

---

# PHASE 15 — REST API Standards

## M2-046 — API versioning [ ]

All public APIs under `/api/v1/`.

---

## M2-047 — Standard API response format [ ]

Success:

```json
{
  "success": true,
  "data": {}
}
```

Error:

```json
{
  "success": false,
  "error": {
    "code": "CAMPAIGN_NOT_FOUND",
    "message": "Campaign was not found."
  }
}
```

No stack traces to clients.

---

## M2-048 — Global exception handling [ ]

Centralize: validation, authentication, authorization, not found, database, SMTP, file upload, business rules.

---

# PHASE 16 — Validation

## M2-049 — Request validation [ ]

Bean Validation for email, password, campaign/template names, SMTP host/port, file type, required fields.

---

# PHASE 17 — Security / Tenant Isolation

## M2-050 — Authorization tests [ ]

User A cannot access User B’s lists, templates, SMTP, campaigns, recipients, or usage. **Mandatory.**

---

## M2-051 — ID enumeration protection [ ]

Always verify organization ownership; do not rely on unguessable IDs.

---

## M2-052 — Sensitive information protection [ ]

APIs never expose `password_hash`, SMTP password, JWT secret, encryption keys, or database credentials.

---

# PHASE 18 — API Documentation

## M2-053 — OpenAPI documentation [ ]

Document auth, contact, template, SMTP, campaign, and usage APIs with request/response examples.

---

# PHASE 19 — Testing

## M2-054 — Repository tests [ ]

Test important repository queries.

---

## M2-055 — Service tests [ ]

Cover registration, login, organization creation, contact import, template rendering, SMTP configuration, campaign creation/lifecycle, usage tracking.

---

## M2-056 — Controller tests [ ]

HTTP status, validation, authentication, authorization, response shape, errors.

---

## M2-057 — Tenant isolation tests [ ]

Explicit tests: Organization A cannot access Organization B resources. **Mandatory.**

---

## M2-058 — Campaign worker tests [ ]

Success, failure, retry, pause, resume, cancel, duplicate prevention, restart where practical. **Mock SMTP. No real emails.**

---

# PHASE 20 — Integration Testing

## M2-059 — PostgreSQL integration tests [ ]

Isolated test database. Prefer Testcontainers if practical. Verify Flyway, JPA, repositories, transactions, constraints.

---

## M2-060 — Full campaign integration test [ ]

```text
Create User
     ↓
Create Organization
     ↓
Upload Contacts
     ↓
Create Template
     ↓
Configure SMTP
     ↓
Create Campaign
     ↓
Start Campaign
     ↓
Worker Processes Recipient
     ↓
Mock SMTP
     ↓
Recipient SENT
     ↓
Campaign COMPLETED
```

---

# PHASE 21 — Actuator / Health

## M2-061 — Spring Boot Actuator [ ]

Safe `health`, `info`, `metrics`. Do not expose secrets or full env dumps.

---

# PHASE 22 — Database Quality

## M2-062 — Add indexes [ ]

Review indexes for users.email, membership FKs, contacts email/org, campaigns org, campaign_recipients campaign_id/status. Only add indexes justified by queries.

---

## M2-063 — Database constraints [ ]

NOT NULL, UNIQUE, foreign keys, check constraints where useful. Do not rely only on Java validation.

---

# PHASE 23 — Transactions

## M2-064 — Review transactional boundaries [ ]

User + organization creation, contact import, campaign creation and state changes, recipient state, usage updates. Avoid huge transactions.

---

# PHASE 24 — Logging

## M2-065 — Structured application logging [ ]

Log auth events, imports, campaign lifecycle, SMTP connection status (not credentials), send result, errors. Never log passwords, JWTs, SMTP secrets, or unnecessary PII.

---

# PHASE 25 — Documentation

## M2-066 — Update README [ ]

Overview, stack, requirements, env vars, database/Flyway, startup, API docs link, testing. Keep CLI batch instructions; add API how-to.

---

## M2-067 — Create API documentation [ ]

```text
docs/API.md
```

---

## M2-068 — Create database documentation [ ]

```text
docs/DATABASE.md
```

Tables, relationships, indexes, tenant model, migration strategy.

---

## M2-069 — Create security documentation [ ]

```text
docs/SECURITY.md
```

Authentication, authorization, tenant isolation, password hashing, SMTP encryption, secret management, API security.

---

# PHASE 26 — Code Quality

## M2-070 — Remove dead code [ ]

Remove only code confirmed unused. Do not delete working CLI paths without a replacement flag.

---

## M2-071 — Review dependencies [ ]

Review Spring Boot, Security, JPA, PostgreSQL, Flyway, Apache POI, CSV library, OpenAPI, test libs. Remove unused. No unrelated major upgrades.

---

## M2-072 — Static analysis [ ]

Compiler warnings, available static analysis/formatting. Fix meaningful issues.

---

# PHASE 27 — Final Verification

## M2-073 — Clean build [ ]

```bash
mvn clean verify
```

Must pass.

---

## M2-074 — Run complete test suite [ ]

Record total / passed / failed / skipped / coverage.

---

## M2-075 — Security verification [ ]

Search the repo for password, secret, token, apikey, SMTP, JWT. Confirm no real credentials are committed.

---

## M2-076 — API smoke test [ ]

Register, login, contact list, Excel upload, template, SMTP config, SMTP test (mock or dry), campaign create/start/pause/resume/cancel/view. No live recipient blasts.

---

# PHASE 28 — Final Architecture Review

## M2-077 — Verify architecture [ ]

Expected:

```text
                 REST API
                    │
                    ▼
              Controllers
                    │
                    ▼
                Services
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
      Repositories  Core     Security
          │         Logic
          ▼
      PostgreSQL
```

Campaign execution:

```text
Campaign API → Campaign Service → Recipient Queue → Background Worker
    → Template Renderer → EmailSender → SMTP
```

Update `docs/ARCHITECTURE.md` if the as-built design differs.

---

# PHASE 29 — Prepare Milestone 3

## M2-078 — Create next milestone preparation [ ]

Create `docs/NEXT_MILESTONE.md` **for Milestone 3** (replace or clearly section the M1-035 CLI handoff so it does not claim M2 is unimplemented after M2 is done).

Document what Milestone 3 should address:

```text
React/Next.js frontend
Login UI
Dashboard
Contact management UI
Excel upload UI
Template editor
SMTP configuration UI
Campaign wizard
Campaign monitoring
Campaign reports
```

Do **not** implement the frontend during Milestone 2.

---

# Definition of Done

Milestone 2 is complete only when:

- [ ] Spring Boot **web** backend is working.
- [ ] PostgreSQL is integrated.
- [ ] Flyway migrations are working.
- [ ] Users can register.
- [ ] Users can authenticate.
- [ ] Organizations exist.
- [ ] Organization membership exists.
- [ ] Tenant isolation is implemented.
- [ ] Contact lists are persisted.
- [ ] Contacts are persisted.
- [ ] Excel/CSV upload works through API.
- [ ] Contact validation works.
- [ ] Templates are persisted.
- [ ] Template variables work.
- [ ] SMTP configurations are persisted securely.
- [ ] SMTP passwords are encrypted.
- [ ] SMTP test endpoint works (tests mocked).
- [ ] Campaigns are persisted.
- [ ] Campaign recipients are persisted.
- [ ] Campaign lifecycle works.
- [ ] Background processing works.
- [ ] Retry handling works.
- [ ] Duplicate sending is prevented.
- [ ] Usage is tracked.
- [ ] API versioning is implemented.
- [ ] DTOs are used.
- [ ] Global exception handling exists.
- [ ] OpenAPI documentation exists.
- [ ] Actuator is configured safely.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Tenant isolation tests pass.
- [ ] No secrets are committed.
- [ ] Full build passes (`mvn clean verify`).
- [ ] Documentation is updated.
- [ ] Milestone 3 requirements are documented.
- [ ] Milestone 1 CLI Excel → SMTP still works: batch off / dry-run on by default; test-send; sent-log; attachments; HTML; delay; fail-loud files; per-recipient errors.
- [ ] CLI still runs **without** PostgreSQL (`WebApplicationType.NONE`).
- [ ] Existing Milestone 1 tests still pass (`mvn -q test` / `mvn clean verify`).
- [ ] `ArchitectureLayeringTest` still enforces excel → template → campaign → smtp (updated only if new packages are documented).
- [ ] Campaign worker does not run on CLI startup.

---

# Final Agent Report

At the end of Milestone 2, the agent MUST produce:

## 1. Summary

What was implemented.

## 2. Completed Tasks

All completed M2 IDs.

## 3. Files Created

## 4. Files Modified

## 5. Database Changes

Tables, indexes, constraints, Flyway migrations.

## 6. API Endpoints

## 7. Security

```text
Authentication: PASS/FAIL
Authorization: PASS/FAIL
Tenant isolation: PASS/FAIL
Password hashing: PASS/FAIL
SMTP encryption: PASS/FAIL
Secrets protection: PASS/FAIL
```

## 8. Tests

```text
Unit tests:
Integration tests:
Controller tests:
Security tests:
Total:
Passed:
Failed:
Skipped:
Coverage:
```

## 9. Build

```text
Build: PASS/FAIL
```

## 10. Known Issues

## 11. Technical Debt

## 12. Milestone 3 Readiness

```text
READY / NOT READY
```

If NOT READY, list blockers.

---

# Critical Agent Rule

Never mark a task `[x]` merely because code has been written.

A task is complete only after:

```text
Implementation → Compilation → Tests → Verification → Documentation → this file updated
```

If verification cannot be completed, mark `[!]` and explain.

Do not use real SMTP credentials or send mail to verify changes.

---

## Suggested order

Work phases in order. One ID per pass unless listed as a pair. After **every** ID: `mvn -q test` must still pass existing Milestone 1 tests (compatibility contract).

1. **Discovery:** M2-001 → M2-002  
2. **Web foundation:** M2-003 → M2-004  
3. **Database:** M2-005 + M2-006, then M2-007 → M2-010  
4. **Auth:** M2-011 → M2-014  
5. **Contacts:** M2-015 + M2-016, then M2-017 → M2-020  
6. **Templates:** M2-021 → M2-023  
7. **SMTP:** M2-024 + M2-025, then M2-026 → M2-027  
8. **Campaigns:** M2-028 + M2-029, then M2-030 → M2-035  
9. **Worker:** M2-036 → M2-043  
10. **Usage:** M2-044 → M2-045  
11. **API quality:** M2-046 → M2-049 (may be started earlier if endpoints already exist; still close IDs in order unless a pair is named)  
12. **Security tests:** M2-050 → M2-052  
13. **OpenAPI:** M2-053  
14. **Tests:** M2-054 → M2-060  
15. **Ops/quality:** M2-061 → M2-072  
16. **Close-out:** M2-073 → M2-078  

Agents: start with [AGENTS.md](AGENTS.md). Project rules: `.cursor/rules/` (mail safety).

# End of DEVELOPMENT-MILESTONE2.md
