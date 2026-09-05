# Milestone 2 analysis (M2-001 / M2-002)

Discovery snapshot of the Milestone 1 CLI before SaaS work. Do not treat this file as an implementation license: follow `DEVELOPMENT-MILESTONE2.md` IDs in order.

Inspected: `pom.xml`, `MailSenderApplication`, `BatchMailRunner`, `MailBody`, `excel.*`, `template.*`, `campaign.*`, `smtp.*`, `config.*`, `application*.properties`, existing tests, `docs/ARCHITECTURE.md`.

---

## Current architecture

Non-web Spring Boot **3.2.3** CLI, Java 17 (`pom.xml`), package root `com.mailSender`.

```text
MailSenderApplication
  WebApplicationType.NONE (hardcoded in main)
  @EnableConfigurationProperties(MailAppProperties, MailProperties)
        ↓
BatchMailRunner (CommandLineRunner)
  test-send XOR Excel batch (batch default off)
        ↓
ExcelReader / ExcelValidator → Contact / ExcelReadResult
        ↓
EmailTemplate + TemplateValidator + TemplateRenderer
        ↓
EmailComposer → EmailMessage
        ↓
EmailSender / SmtpEmailSender  (dry-run skips JavaMailSender)
        ↓
SentAddressLog (file) + MailBodyAttachment (optional file)
```

Safe defaults (`application.properties`): `mail.batch-enabled=false`, `mail.dry-run=true`, `mail.test-send-enabled=false`. Credentials via env / gitignored `application-local.properties`. Profiles `development` / `production` remain.

There is **no** `starter-web`, JPA, Flyway, Security, Actuator, PostgreSQL driver, or `/api/v1` surface.

---

## Reusable components

| Component | Role in Milestone 2 |
| --- | --- |
| `excel.ExcelReader` / `ExcelValidator` / `Contact` / `ExcelReadResult` | API `.xlsx` import **as-is** (headers, `@` check, keep-first duplicates, `DataFormatter`). Summary counts map to import API. |
| New CSV importer (not yet present) | Upload API only; **do not** fold CSV into `ExcelReader`. |
| `template.EmailTemplate` / `TemplateRenderer` / `TemplateValidator` | Persist body/subject; validate `{{placeholders}}` against contact columns. |
| `campaign.EmailMessage` / `EmailComposer` | Worker compose path; keep campaign package free of SMTP imports (`ArchitectureLayeringTest`). |
| `smtp.EmailSender` / `SmtpEmailSender` / `SmtpFailureClassifier` | Transport; dry-run must stay; map stored SMTP into `SmtpConfiguration` at send time. |
| `config.MailAppProperties` / `SmtpConfiguration` | Keep `mail.*` / `spring.mail.*` names and defaults. Add keys; do not rename. Password masking in `toString()` stays. |
| `BatchMailRunner` / `MailBody` / `SentAddressLog` / `MailBodyAttachment` | CLI one-shot path. Worker should call compose/send types, not delete CLI. API campaigns use `campaign_recipients`, not the file log. |
| Tests listed in the compatibility contract | Gate after every M2 ID. Mock `JavaMailSender` / `EmailSender`; no live SMTP. |

---

## Components requiring adaptation

| Component | Why |
| --- | --- |
| `MailSenderApplication` | Always `NONE`. API profile must use servlet **without** making CLI servlet-only. |
| `BatchMailRunner` | Must not send when the API process is running. Gate with `!api` (or equivalent). |
| Auto-configuration | Adding JPA/Flyway/Security/web must be **profile-gated** so `@SpringBootTest(NONE)` and `mvn spring-boot:run` need no Postgres. |
| `SmtpEmailSender` | CLI uses Boot `JavaMailSender` + `mail.dry-run`. API needs per-tenant decrypted SMTP mapped into `SmtpConfiguration` / a sender factory; do not log passwords. |
| `EmailComposer` | Uses global `mail.subject` / attachment path. Worker should pass campaign template subject and optional attachments without rewriting CLI defaults. |
| Package layout | Add `auth`, `user`, `organization`, `contact`, `usage`, `common.*`, worker **outside** `campaign` if the worker imports `smtp` (layering test forbids `campaign` → `smtp`). |
| `ArchitectureLayeringTest` | Extend only when new packages are documented; keep excel → template → campaign → smtp one-way. |

---

## Components requiring replacement

None of the Milestone 1 pipeline types should be replaced.

Replace **as a source of truth for API campaigns only**:

- File paths (`mail.excel-file-path`, `mail.body-file-path`) → Postgres contact lists / templates.
- `SentAddressLog` file → `campaign_recipients` status (CLI file log remains for CLI).
- Process-lifetime `CommandLineRunner` send loop → HTTP create/start + background worker (API profile only).

Do **not** replace POI Excel rules, placeholder rendering, or `EmailSender`.

---

## Potential risks

1. **Classpath auto-config:** PostgreSQL driver / Flyway / Security on the classpath can break CLI context tests unless excluded outside the `api` profile.
2. **Hardcoded `WebApplicationType.NONE`:** Forgetting a dual-mode entrypoint either starts Tomcat on CLI or prevents the API from binding a port.
3. **Worker on CLI:** A `@Scheduled` worker without `api` gating would poll/send on `spring-boot:run`.
4. **Layering:** Putting the campaign worker under `campaign` and importing `smtp` fails `ArchitectureLayeringTest`.
5. **Tenant isolation:** Resource IDs in URLs must always be checked against organization membership.
6. **SMTP secrets:** Persist ciphertext only; never return passwords; encryption key from env.
7. **Email validation:** Shared `@` check must not be tightened in a way that changes CLI skip counts.
8. **Test order / logging:** `ExcelReaderTest.logsLoadCountsWithoutPasswords` failed once when a ListAppender missed INFO logs after Spring Boot tests reconfigured Logback (see baseline). Not a functional SMTP/Excel break.
9. **I1:** Any historically leaked Gmail App Password must be rotated in Google by the operator; do not rewrite git history unless asked.

---

## M2-002 baseline (`mvn -q test`)

| | |
| --- | --- |
| Date | 2026-09-05 |
| Command | `mvn -q test` |
| Live SMTP | No (`mail.batch-enabled=false` / mocks; `MAIL_PASSWORD` not required) |
| First run | **124** tests, **1** failure, **0** errors, **0** skipped |
| Failure | `ExcelReaderTest.logsLoadCountsWithoutPasswords` — ListAppender did not see `Excel file loaded: log-counts.xlsx` (log-capture after Spring context tests; reader still returns valid contacts) |
| Follow-up | `mvn -q test` **124** tests, **0** failures after setting Logback INFO on the ExcelReader logger in that test. Gate is green. |

The CLI Excel → template → SMTP pipeline is **not** fundamentally broken. Milestone 2 may proceed.

JaCoCo still reports `excel` / `template` / `campaign` after `mvn test` (M1-032 scope).

## Authentication decisions (M2-013)

Until `docs/SECURITY.md` (M2-069):

- Passwords: BCrypt via `PasswordEncoder`.
- Tokens: HMAC JWT (`APP_JWT_SECRET`, ≥32 characters, `excelmail.security.jwt-expiration-ms`).
- Authorization: authenticated JWT; organization membership checked in services (`TenantService`).
- Rate limit: in-memory per-client key on register/login (`excelmail.auth.rate-limit-per-minute`).
- APIs never return `password_hash`.

