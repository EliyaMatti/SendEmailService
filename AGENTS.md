# ExcelMail Pro — AI Agent Development Rules

This file is the permanent rule set for AI coding agents on this repository (SendEmailService / ExcelMail Pro). These rules apply to all milestones unless a milestone document explicitly overrides them.

## Active work

| Document | Role |
| --- | --- |
| [`DEVELOPMENT_TASKS.md`](DEVELOPMENT_TASKS.md) | Milestone 1 (M1-001–M1-035). Historical / complete unless an ID is still open. |
| [`DEVELOPMENT-MILESTONE2.md`](DEVELOPMENT-MILESTONE2.md) | **Active:** Milestone 2 SaaS backend (M2-001–M2-078). |
| [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md) | Milestone 1 per-task loop. For Milestone 2, follow the loop in this file and in `DEVELOPMENT-MILESTONE2.md`. |
| [`docs/NEXT_MILESTONE.md`](docs/NEXT_MILESTONE.md) | M1-035 handoff notes. Do not treat it as a substitute for `DEVELOPMENT-MILESTONE2.md`. |

Cursor rules in [`.cursor/rules/`](.cursor/rules/) always apply, especially mail safety: no live SMTP to verify changes; no secrets in git.

---

# 1. Purpose

Agents must understand the repository, follow the **active milestone**, make the smallest safe change, test without sending real mail, then document.

---

# 2. First Rule — Understand Before Changing

Before modifying the project:

1. Inspect the repository.
2. Read `README.md`.
3. Read the active milestone document (`DEVELOPMENT-MILESTONE2.md` unless the user names Milestone 1).
4. Inspect the existing architecture (`docs/ARCHITECTURE.md`, `docs/PROJECT_ANALYSIS.md`).
5. Identify reusable Milestone 1 code.
6. Identify existing tests.
7. Run `mvn -q test` before major changes.

Never assume the project structure. Milestone 1 left a **non-web** Spring Boot CLI (`WebApplicationType.NONE`). Do not assume APIs, PostgreSQL, or Security already exist.

---

# 3. Milestone Discipline

The active milestone controls what the agent is allowed to implement.

```text
DEVELOPMENT_TASKS.md
        ↓
Only Milestone 1 tasks (M1-*)

DEVELOPMENT-MILESTONE2.md
        ↓
Only Milestone 2 tasks (M2-*)

Later milestone documents
        ↓
Only that milestone
```

Do **not** implement frontend, billing, tracking pixels, or other future-milestone features unless explicitly instructed.

Do **not** restart Milestone 1 discovery (M1-001) when Milestone 2 is the assigned work.

---

# 4. Task Execution

For every task ID:

```text
Read task
   ↓
Mark [~]
   ↓
Inspect existing code
   ↓
Implement
   ↓
Compile
   ↓
Run tests (mvn -q test; mvn clean verify before milestone close)
   ↓
Verify (no real SMTP)
   ↓
Document (README / docs / config examples)
   ↓
Mark [x]
```

If blocked: mark `[!]` and explain the blocker.

Never mark a task complete merely because code was written.

One ID per pass unless `DEVELOPMENT-MILESTONE2.md` names a tightly coupled pair.

---

# 5. Minimal Change Principle

Prefer the smallest change that correctly solves the task.

Do NOT:

- Rewrite working Milestone 1 modules without reason.
- Rename large numbers of files unnecessarily.
- Introduce Kafka, Redis, or extra frameworks unless the active milestone requires them.
- Refactor unrelated code.
- Upgrade dependencies without justification.
- Delete working CLI Excel → SMTP functionality.

---

# 6. Preserve Existing Functionality

ExcelMail Pro's core functionality is:

```text
Excel/CSV
   ↓
Contact Data
   ↓
Email Template
   ↓
Rendered Email
   ↓
SMTP
   ↓
Recipient
```

This workflow must remain functional. Prefer wrapping Milestone 1 types rather than replacing them:

| Milestone 1 type | Role to reuse |
| --- | --- |
| `com.mailSender.excel.ExcelReader` / `ExcelValidator` / `Contact` | Import and validation |
| `com.mailSender.template.*` | Body, `{{placeholders}}`, validation |
| `com.mailSender.campaign.EmailMessage` / `EmailComposer` | Compose without JavaMail types |
| `com.mailSender.smtp.EmailSender` / `SmtpEmailSender` | Transport; dry-run must stay available |
| `com.mailSender.config.MailAppProperties` / `SmtpConfiguration` | Map to stored settings; never log passwords |
| `BatchMailRunner` / `MailBody` | CLI one-shot send; a worker can call the same loop |

Keep `mail.batch-enabled=false` as the safe default. Startup must not send a campaign when the web API is enabled.

**Do not change Milestone 1 CLI behavior.** Add API + Postgres behind an explicit API profile. Default `mvn spring-boot:run` and existing `@SpringBootTest(NONE)` tests must not require a database, Tomcat, or JWT. Full freeze list: `DEVELOPMENT-MILESTONE2.md` → Compatibility contract.

Do not alter `mail.*` / `spring.mail.*` key names or safe defaults. Do not remove test-send, `SentAddressLog`, attachments, or `ExcelReader` `.xlsx` rules. Add CSV as a separate importer. Do not tighten shared email validation in a way that changes CLI skip counts. Do not run the campaign worker in CLI mode.

---

# 7. Architecture Principles

Prefer clear separation:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Business logic belongs in services, not controllers.

Controllers should primarily handle:

- HTTP requests
- Validation
- Authentication context
- Response mapping

Do not put Apache POI or JavaMail in controllers.

Package root remains `com.mailSender` unless a later task explicitly migrates it. Add packages (`auth`, `organization`, `contact`, `campaign`, `usage`, `common`, …) under that root; do not invent a parallel `com.excelmailpro` tree.

---

# 8. DTO Rule

Never expose JPA entities directly from REST APIs.

Use:

```text
Request DTO → Service → Entity
Entity → Service → Response DTO
```

---

# 9. Database Rules

PostgreSQL is the primary application database for Milestone 2.

All schema changes MUST use Flyway under `src/main/resources/db/migration/`.

Never manually modify production schema.

Never modify an already-applied migration. Create a new migration instead.

Example:

```text
V1__create_users.sql
V2__create_organizations.sql
V3__create_contacts.sql
```

Never put secrets in SQL.

---

# 10. Tenant Isolation

ExcelMail Pro is a multi-tenant SaaS application.

Organization A must never access Organization B's:

- Contacts
- Contact lists
- Templates
- SMTP accounts
- Campaigns
- Campaign recipients
- Usage data

Never trust a resource ID by itself. Always verify organization membership.

---

# 11. Security Rules

Never commit:

- Passwords
- API keys
- SMTP credentials
- JWT secrets
- Encryption keys
- Database passwords
- Private keys

Do not commit `application-local.properties` or `.env` (gitignored). Use environment variables or secure configuration. Examples stay placeholders (`MAIL_PASSWORD`, `DB_PASSWORD`, JWT/encryption keys).

Never log secrets. Never return secrets through APIs.

For **I1** (rotate a leaked Gmail App Password): remind the user to revoke it in Google. Do not rewrite git history unless they explicitly ask.

---

# 12. Password Security

User passwords must never be stored as plaintext.

Use BCrypt (or another Spring Security-supported password hasher). Never invent custom password encryption.

---

# 13. SMTP Security

SMTP passwords are sensitive:

- Encrypt credentials at rest.
- Do not expose them through REST APIs (write-only).
- Do not log them (`SmtpConfiguration` already masks passwords).
- Do not include them in exception messages.
- Do not commit them.
- Encryption keys come from environment / secure config.

---

# 14. Email Safety

ExcelMail Pro is for legitimate business communication.

The agent must NOT implement features designed to:

- Bypass SMTP limits.
- Evade provider restrictions.
- Hide sender identity.
- Circumvent spam controls.
- Send unsolicited spam.
- Rotate accounts to evade limits.
- Automatically create accounts.
- Abuse third-party email infrastructure.

Sending limits must remain provider-compliant.

Do not call real SMTP to verify changes. Use dry-run, `mail.batch-enabled=false`, and mocked `JavaMailSender` / `EmailSender` in tests.

---

# 15. Email Sending Architecture

Prefer:

```text
Campaign
    ↓
Recipient
    ↓
Template Renderer
    ↓
EmailMessage
    ↓
EmailSender
    ↓
SMTP
```

Do not mix database logic, Excel parsing, template rendering, and SMTP inside one large method.

---

# 16. Background Processing

Email campaigns must not depend on a long-running HTTP request.

Prefer:

```text
REST API → Create Campaign → Persist Recipients → Background Worker → Send Emails
```

A simple Spring scheduling/worker approach is acceptable in Milestone 2.

Do not introduce Kafka unless the active milestone requires it.

---

# 17. Idempotency

Protect against duplicate sends from:

- Application restart
- HTTP retries
- Worker retries
- Database transaction retries
- Worker crashes

Do not assume a process runs exactly once.

---

# 18. Error Handling

Never expose stack traces to API clients.

Log technical errors internally. Return user-friendly messages and stable error codes.

---

# 19. Logging

Log important events (startup, registration, import, campaign lifecycle, send success/failure).

Never log passwords, SMTP passwords, JWT tokens, encryption keys, or database passwords.

Avoid logging complete personal contact records.

---

# 20. Validation

Validate at API, business, and database layers. Do not rely on a future frontend.

---

# 21. Testing Requirements

Prefer unit, integration, controller, and security tests.

Do not send real emails from automated tests. Mock SMTP / `EmailSender`.

---

# 22. Test Quality

Do not write tests only to raise coverage. Verify behavior, especially:

- Authentication and authorization
- Tenant isolation
- Excel parsing
- Template rendering
- SMTP handling (mocked)
- Campaign lifecycle
- Retry and duplicate prevention
- Usage tracking

---

# 23. Build Verification

After meaningful changes: compile, then `mvn -q test`.

Before declaring the milestone complete: `mvn clean verify`.

---

# 24. Dependency Management

Before adding a dependency, ask whether Spring Boot already provides it, whether an existing library (Apache POI, JavaMail) can be reused, and whether it is maintained.

Avoid dependency bloat. Do not blindly upgrade Java or Spring Boot.

This project currently targets **Java 17+**. Do not jump to Java 21 unless a task explicitly requires it and the build is updated.

---

# 25. API Standards

Public REST APIs use `/api/v1/`.

Use GET, POST, PUT/PATCH, DELETE appropriately.

---

# 26. API Error Standards

Use consistent error bodies, for example:

```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "The requested resource was not found."
  }
}
```

Do not expose internal database errors or stack traces.

---

# 27. Pagination

Paginate campaigns, contacts, contact lists, templates, and usage history. Do not load unbounded result sets.

---

# 28. File Upload Security

For Excel/CSV uploads:

- Validate type and size (do not trust the extension alone).
- Validate content.
- Prevent path traversal.
- Do not execute uploaded files.
- Clean up temporary files.

Milestone 1 import is `.xlsx` only. Milestone 2 may add CSV; reuse `ExcelReader` for `.xlsx`.

---

# 29. Configuration

Do not scatter environment-specific values in business logic.

Use application configuration for database, SMTP, sending limits, file limits, JWT, encryption, and app settings.

Keep `application-local.properties.example` in sync; never commit real values.

---

# 30. Code Style

Prefer clear naming, small methods, constructor injection, and meaningful exceptions.

Avoid giant classes, deep nesting, magic numbers, static mutable state, and duplicated business logic.

---

# 31. Documentation

When architecture or behavior changes, update:

```text
README.md
docs/ARCHITECTURE.md
docs/API.md
docs/DATABASE.md
docs/SECURITY.md
```

---

# 32. Git Discipline

Do not commit unless the user asks.

Do not commit `.env`, secrets, credentials, IDE junk, or `target/` build artifacts.

Recommended baseline tag/commit name before large Milestone 2 work (only if the user asks to commit): `baseline-before-milestone-2`.

---

# 33. Agent Communication

When reporting progress:

```text
Task
Status
Files changed
Implementation summary
Tests executed
Problems encountered
```

Do not claim something works without verification.

---

# 34. Blocker Handling

If a task cannot be completed, mark `[!]` and document:

```text
Blocker:
Why it happened:
What was attempted:
What is required:
```

Do not silently skip the task.

---

# 35. Future Milestones

Do not implement prematurely:

```text
Milestone 3 — Frontend
Milestone 4 — Billing + Licensing
Milestone 5 — Production + Launch
Milestone 6 — Advanced analytics/features
```

---

# 36. Final Verification

Before declaring a milestone complete:

```text
All tasks reviewed
        ↓
Build passes
        ↓
Tests pass
        ↓
Security checked
        ↓
Documentation updated
        ↓
Git status reviewed
        ↓
Final report generated
```

Milestone 2 is done only when `DEVELOPMENT-MILESTONE2.md` Definition of Done is satisfied and `mvn clean verify` is green.

---

# 37. Golden Rule

When uncertain:

> Understand the existing code first. Make the smallest safe change. Test it. Verify it. Document it.

Optimize for correctness, security, maintainability, testability, and simplicity — not volume of code.

---

## Cloud / local agent environment

- Tests: `mvn -q test` (batch stays off in context tests).
- Do not send mail: `MAIL_BATCH_ENABLED=false` or `MAIL_DRY_RUN=true`; mock `JavaMailSender`.
- Real sends need `MAIL_USERNAME` / `MAIL_PASSWORD` (or gitignored `application-local.properties`); never commit credentials.
- Postgres for Milestone 2: `DB_*` env vars; never commit `DB_PASSWORD`.

## Start here (Milestone 2)

1. Read this file and [`DEVELOPMENT-MILESTONE2.md`](DEVELOPMENT-MILESTONE2.md).
2. Follow **Suggested order** at the bottom of that file.
3. Discovery first: M2-001 then M2-002 before Spring Boot/web/database work.
4. One incomplete ID per pass unless the milestone names a tightly coupled pair.

# END OF AGENTS.md
