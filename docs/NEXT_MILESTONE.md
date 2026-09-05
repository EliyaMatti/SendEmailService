# Next milestone (after ExcelMail Pro Milestone 1)

This file is **documentation only** (M1-035). Do **not** implement the items below in Milestone 1. The current product remains a **non-web** Spring Boot CLI (`WebApplicationType.NONE`): Excel → `Contact` → template → `EmailMessage` → `EmailSender` / `SmtpEmailSender`. See [ARCHITECTURE.md](ARCHITECTURE.md).

Milestone 2 is a SaaS-shaped **API + persistence** layer that should **reuse** that pipeline, not replace it with a new mail stack.

## What exists today (keep)

| Milestone 1 type | Role to reuse |
| --- | --- |
| `excel.ExcelReader` / `ExcelValidator` / `Contact` | Import lists; validation rules |
| `template.*` | Body load, `{{placeholders}}`, validation |
| `campaign.EmailMessage` / `EmailComposer` | Compose without JavaMail types |
| `smtp.EmailSender` / `SmtpEmailSender` | Transport; dry-run must stay available |
| `config.MailAppProperties` / `SmtpConfiguration` | Map to stored settings later; never log passwords |
| `BatchMailRunner` / `MailBody` | CLI one-shot send; a worker job can call the same loop |

Do not add tracking pixels, open/click tracking, spam-bypass, or SMTP-limit bypass. Credentials stay in env / secret store, never in git.

## Required for the next phase

### Spring Boot API

Today there is no `spring-boot-starter-web` and no HTTP surface. Milestone 2 needs a web (or reactive) Spring Boot app: `WebApplicationType.SERVLET` (or equivalent), JSON error handling, and a split from `CommandLineRunner` so startup does not send a campaign. Keep the CLI module or a `mail.batch-enabled` profile if operators still need Excel-from-disk runs.

### PostgreSQL

Replace file-backed inputs (xlsx path, body path, `sent-addresses.txt`) with a database for users, campaigns, contacts, templates, and SMTP settings. Use a managed or local Postgres instance. Do not store App Passwords in application properties committed to git.

### Flyway

Versioned SQL migrations for schema changes. First migrations should create the tables implied below. Keep migrations in source control; never put secrets in SQL.

### REST endpoints

Typical first resources (illustrative, not implemented):

- Auth: register / login / token refresh (see Authentication)
- Campaigns: create, list, get, start/cancel
- Contacts: upload Excel or JSON, list, delete
- Templates: CRUD for subject/body
- SMTP config: save host/port/username; password write-only
- Jobs: status of a send run (queued / running / sent / failed)

Map HTTP to existing domain types (`Contact`, `EmailMessage`) inside application services. Do not put POI or JavaMail in controllers.

### Authentication

The CLI has no users. Milestone 2 needs authenticated tenants (session or JWT) and authorization so one account cannot send another account’s list. Store password hashes (never SMTP App Passwords as login passwords). Out of scope until designed: OAuth social login, SSO.

### Campaign persistence

A campaign row: owner, subject, template id, attachment reference, flags (html, delay), status, timestamps. The in-memory loop in `MailBody` becomes “load campaign + contacts + skip already-sent from DB, then `EmailSender.send`.” Do not mail the full Excel file from disk as the only source of truth.

### Contact persistence

Store email, name, extra placeholder columns, campaign or list id, and send state (pending / sent / failed / skipped). Excel upload can still use `ExcelReader` then insert rows. Duplicate-email policy should match Milestone 1 (keep first) unless product changes it and documents it.

### Template persistence

Store UTF-8 body (and subject if templated later) per campaign or as shared templates. Keep `TemplateRenderer` / `TemplateValidator`; validation still uses placeholder keys from contact columns.

### SMTP configuration persistence

Per-user or per-campaign host, port, TLS, username, from address. **Password** in a secret column or external secret manager; never return it on GET; never log it (`SmtpConfiguration.toString()` already uses `password=***`). Runtime still builds `JavaMailSender` / `SmtpConfiguration` from stored values.

### Background email processing

The CLI sends in the startup thread and exits. Milestone 2 needs an async worker (Spring `@Async`, a job table + poller, or a queue). Respect `mail.send-delay-ms`, per-recipient failure isolation, and dry-run for staging. Do not fire SMTP from an HTTP request thread for large lists. Provider rate limits still apply; do not add bypass logic.

## Suggested order (Milestone 2, not started)

1. Add web starter and health/info without sending mail.
2. Postgres + Flyway empty schema.
3. Authentication and tenant isolation.
4. Persist templates, contacts, campaigns, SMTP settings.
5. Background worker that calls `EmailSender` with dry-run default in non-prod.
6. REST for operators to start a campaign.

React UI, payments, subscriptions, licenses, Kafka, Redis, AI generation, and CRM are listed as out of scope for Milestone 1 and are **not** required to begin the list above.

## Blockers and leftovers (honest)

- This repo is still CLI-only until someone adds a web stack.
- `MailBody` / `MailBodyAttachment` / `SentAddressLog` remain in the application root (M1-034); fine to move when introducing a `campaign` service.
- I1: if an App Password was ever in git history, the user must revoke it in Google.
- Default subject and Gmail host in `application.properties` are CLI conveniences; SaaS defaults should be generic and per-tenant.

Milestone 2 readiness of the **core library** (Excel → SMTP types) is **yes**. Readiness of a **running SaaS** is **no** until the items in this file are implemented in a later milestone.
