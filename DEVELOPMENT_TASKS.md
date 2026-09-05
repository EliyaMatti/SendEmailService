# ExcelMail Pro — Milestone 1 Tasks

## Milestone Objective

Transform the existing Excel → SMTP email sender into a clean, modular, testable foundation that can later be integrated into the ExcelMail Pro SaaS platform.

### Milestone 1 Scope

This milestone is ONLY about:

* Understanding the existing application
* Refactoring the existing code safely
* Separating responsibilities
* Improving Excel processing
* Improving email template handling
* Improving SMTP sending
* Adding validation
* Adding logging
* Adding configuration management
* Adding tests
* Creating clear documentation

### Explicitly OUT OF SCOPE

Do NOT implement:

* Spring Boot SaaS APIs
* React frontend
* User authentication
* PostgreSQL
* Payment integration
* Subscription management
* License management
* Cloud deployment
* Kafka
* Redis
* AI email generation
* CRM functionality
* Marketing automation
* Email tracking pixels
* Open tracking
* Click tracking
* Spam-bypass functionality
* SMTP-limit bypassing

---

# 1. Agent Operating Rules

The agent MUST follow these rules throughout this milestone.

## Rule 1 — Inspect before modifying

Before changing code:

1. Inspect the complete project structure.
2. Identify the application entry point.
3. Identify Excel processing code.
4. Identify SMTP/email sending code.
5. Identify configuration files.
6. Identify dependencies.
7. Identify existing tests.
8. Identify hardcoded credentials or secrets.
9. Identify duplicated logic.
10. Identify error-handling weaknesses.

Do NOT immediately rewrite the application.

---

## Rule 2 — Preserve existing behavior

The existing application is considered the baseline.

Before refactoring:

* Run the application.
* Run existing tests if available.
* Verify the current workflow.
* Record the current behavior.

After each major refactoring:

* Build the application.
* Run tests.
* Verify the original workflow still works.

Do not introduce breaking changes unless necessary.

---

## Rule 3 — Small changes only

Work task-by-task.

After completing each task:

1. Build.
2. Run tests.
3. Check for compilation errors.
4. Review changed files.
5. Update this `DEVELOPMENT_TASKS.md`.
6. Only then continue.

---

## Rule 4 — Never expose credentials

Never commit:

* SMTP passwords
* API keys
* Tokens
* Private keys
* Real email credentials

If credentials are currently hardcoded, replace them with environment/configuration variables during this milestone.

---

# 2. Task Status Convention

Use:

* `[ ]` Not started
* `[~]` In progress
* `[x]` Completed
* `[!]` Blocked

The agent MUST update task status after completion.

---

# 3. Phase 1 — Project Discovery

## M1-001 — Inspect project structure [x]

* [x] Identify programming language/version.
* [x] Identify build system.
* [x] Identify application entry point.
* [x] Identify source directories.
* [x] Identify resources/configuration.
* [x] Identify test directories.
* [x] Identify dependency files.
* [x] Identify documentation.
* [x] Identify generated/build files.

### Deliverable

Create:

`docs/PROJECT_ANALYSIS.md`

Include:

```text
Technology Stack
Application Entry Point
Build System
Current Architecture
Excel Processing Location
SMTP Processing Location
Configuration Location
Testing Status
Known Problems
Refactoring Recommendations
```

---

# 4. M1-002 — Establish baseline [x]

* [x] How the application starts.
* [x] Required input files.
* [x] Required configuration.
* [x] Excel format.
* [x] Email format.
* [x] SMTP configuration.
* [x] Expected output.
* [x] Error behavior.

### Deliverable

`docs/BASELINE.md` — current Excel → SMTP workflow (verified with `mvn test` and dry-run / batch-off `spring-boot:run`; no live SMTP).

---

# 5. M1-003 — Identify technical debt [x]

* [x] God classes
* [x] Large methods
* [x] Duplicate code
* [x] Hardcoded values
* [x] Hardcoded credentials
* [x] Static state
* [x] Tight coupling
* [x] Poor exception handling
* [x] Poor logging
* [x] Unclear naming
* [x] Unused dependencies
* [x] Unused methods
* [x] Magic numbers
* [x] Magic strings

### Deliverable

`docs/TECHNICAL_DEBT.md` — findings only; no fixes in this task.

---

# 6. Phase 2 — Define the Core Architecture

## M1-004 — Define core modules [x]

* [x] Map the ExcelMail Pro module sketch to the existing CLI (do not create empty packages).
* [x] Document current types vs target modules (`excel`, `template`, `smtp`, `campaign`, `config`, `application`).
* [x] Record what stays out of Milestone 1 (SaaS, extra logging facade).

The application should eventually have clear responsibilities similar to:

```text
ExcelMailPro
│
├── config
│
├── excel
│   ├── ExcelReader
│   ├── ExcelValidator
│   └── Contact
│
├── template
│   ├── EmailTemplate
│   └── TemplateRenderer
│
├── smtp
│   ├── SmtpConfiguration
│   └── EmailSender
│
├── campaign
│   └── EmailCampaign
│
├── validation
│
├── logging
│
└── application
```

Do not blindly create these packages/classes.

Adapt the structure to the existing project.

Document the proposed architecture in:

`docs/ARCHITECTURE.md` — proposed modules adapted to `com.mailSender`; no package move in this task.

---

# 7. M1-005 — Separate Excel processing [x]

* [x] Extract Excel read (headers → rows → contacts) out of campaign/SMTP.
* [x] Domain model `Contact` (email, name, extra columns as placeholders).
* [x] No send/SMTP/UI in the Excel package.

Extract Excel-related responsibilities from the existing code.

The Excel layer should be responsible for:

```text
Read Excel
    ↓
Identify headers
    ↓
Read rows
    ↓
Convert rows to contacts
```

It should NOT:

* Send emails
* Manage SMTP
* Contain UI logic
* Contain campaign logic

Create an appropriate domain model such as:

```text
Contact
```

Minimum expected fields:

```text
email
name
```

Additional columns should be supported where practical.

---

# 8. M1-006 — Excel validation [x]

* [x] Missing file / unsupported type / empty workbook fail with a clear error.
* [x] Missing email column, empty email, invalid format, empty rows: skip row, do not crash.
* [x] Duplicate emails counted; first valid row kept.
* [x] Report `Total rows` / `Valid` / `Invalid` / `Duplicates` (`ExcelReadResult`).

Implement validation for:

* Missing file
* Unsupported file type
* Empty Excel file
* Missing email column
* Empty email
* Invalid email format
* Duplicate email
* Empty rows

The application must not crash because of a malformed row.

Return/report meaningful validation results.

Example:

```text
Total rows: 500
Valid: 472
Invalid: 18
Duplicates: 10
```

---

# 9. M1-007 — Separate email template processing [x]

* [x] `EmailTemplate` loads UTF-8 body files.
* [x] `TemplateRenderer` fills `{{placeholders}}` from `Contact` (any extra columns; mixed-case keys).
* [x] Unknown placeholders render as empty (safe); `$` / `\` in values stay literal.

Extract email-template functionality.

Create a clear abstraction for:

```text
EmailTemplate
TemplateRenderer
```

Support placeholders such as:

```text
{{Name}}
{{Email}}
{{Company}}
```

The implementation must not assume only one fixed field.

Unknown placeholders should be handled safely.

Example:

```text
Template:
Hi {{Name}},

Welcome to {{Company}}.
```

should become:

```text
Hi Rahul,

Welcome to ABC Ltd.
```

---

# 10. M1-008 — Template validation [x]

* [x] Empty subject / empty body fail with a clear message.
* [x] Invalid `{{placeholder}}` syntax fails.
* [x] Missing required fields (`email` / `name` in imported keys) fail.
* [x] Unsupported placeholders fail: `Placeholder {{Company}} does not exist in the imported data.`

Implement validation for:

* Empty subject
* Empty body
* Invalid placeholder syntax
* Missing required fields
* Unsupported placeholders

Provide useful error messages.

Example:

```text
Template validation failed:
Placeholder {{Company}} does not exist in the imported data.
```

---

# 11. Phase 3 — SMTP Refactoring

# M1-009 — Extract SMTP configuration [x]

* [x] Dedicated `SmtpConfiguration` (`host`, `port`, `username`, `password`, `fromEmail`, `fromName`, `tlsEnabled`).
* [x] Bound from existing `spring.mail.*` / `mail.from` (plus optional `mail.from-name`).
* [x] Password never included in `toString()` / logs.
* [x] Batch preflight uses `SmtpConfiguration.isReadyForSend()` (same user-facing error as before).

Create a dedicated configuration model.

Example conceptual structure:

```text
SmtpConfiguration

host
port
username
password
fromEmail
fromName
tlsEnabled
```

Do not expose the password in logs.

---

# M1-010 — Remove hardcoded credentials [x]

* [x] Searched the project for `password` / `smtp` / `username` / `token` / `secret` / `apikey`.
* [x] No live credentials in committed source (placeholders and test fakes only).
* [x] SMTP settings stay env/`application-local.properties` (gitignored); `.env.example` added; no real `.env`.
* [x] `SMTP_*` aliases accepted when matching `MAIL_*` vars are unset.

Search the entire project for:

```text
password
smtp
username
token
secret
apikey
```

If real credentials are found:

* Remove them from source code.
* Move configuration to environment variables/configuration.
* Add an example configuration file if required.

Example:

```text
SMTP_HOST=
SMTP_PORT=
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM_EMAIL=
```

Create:

`.env.example`

Do NOT create a real `.env` containing credentials.

---

# M1-011 — Extract EmailSender [x]

* [x] `EmailSender` interface; `SmtpEmailSender` is the SMTP implementation.
* [x] `MailBody` depends on `EmailSender`, not a concrete SMTP type.
* [x] Dry-run stays a flag inside `SmtpEmailSender` (no `JavaMailSender` call).
* [x] Removed concrete `EmailService`.

Create a dedicated interface:

```java
EmailSender
```

with an implementation such as:

```java
SmtpEmailSender
```

The rest of the application should depend on the abstraction rather than directly constructing SMTP implementation details.

Conceptually:

```text
Application
     ↓
EmailSender
     ↓
SmtpEmailSender
     ↓
SMTP Provider
```

---

# M1-012 — SMTP error handling [x]

* [x] Maps authentication, connection, timeout, invalid recipient, SMTP rejection, and configuration failures.
* [x] Throws `SmtpSendException` with a short operator message (cause kept for debug logs, not printed as a stack trace to the user).
* [x] Batch still continues after one send failure and logs the classified message.

Handle common failures gracefully:

* Authentication failure
* Connection failure
* Timeout
* Invalid recipient
* SMTP rejection
* Configuration error

The application must return useful errors instead of raw stack traces to the user.

---

# 12. Phase 4 — Email Sending Model

# M1-013 — Create email message model [x]

* [x] Domain object `EmailMessage` (`to`, `subject`, `body`, `from`, `replyTo`, `attachments`).
* [x] Independent of SMTP/JavaMail types (`campaign` package).
* [x] `EmailSender.send(EmailMessage)`; `SmtpEmailSender` maps the model onto MIME.

Create a clean domain object representing an email.

Conceptually:

```text
EmailMessage

to
subject
body
from
replyTo
attachments
```

Keep this independent from SMTP implementation details.

---

# M1-014 — Separate email generation from email sending [x]

* [x] Pipeline: Contact → TemplateRenderer (`EmailComposer`) → `EmailMessage` → `EmailSender`.
* [x] Compose and SMTP send are separate methods/types (no MIME in the composer).

The application should follow:

```text
Excel Row
    ↓
Contact
    ↓
TemplateRenderer
    ↓
EmailMessage
    ↓
EmailSender
    ↓
SMTP
```

Email creation and SMTP sending must NOT be mixed in one large method.

---

# M1-015 — Add test-send functionality [x]

* [x] `MailBody.sendTestEmail` sends one rendered message to a specified address and returns success/failure.
* [x] Uses configured SMTP (or dry-run); does not send the Excel list even if batch is on.
* [x] `mail.test-send-enabled` / `mail.test-send-to` (`MAIL_TEST_SEND_ENABLED` / `MAIL_TEST_SEND_TO`).

Create a clear method/workflow for sending one test email.

The test email must:

* Use the configured SMTP account.
* Render the selected template.
* Send to a specified test address.
* Return success/failure.

This must NOT automatically send the full Excel list.

---

# 13. Phase 5 — Configuration

# M1-016 — Centralize configuration [x]

Remove scattered configuration values.

Centralize:

* [x] SMTP settings (`spring.mail.*` → Spring `MailProperties` / `SmtpConfiguration`)
* [x] Input file location (`mail.excel-file-path`, `mail.body-file-path`, `mail.attachment-path`, `mail.sent-log-path`)
* [x] Logging configuration (`logging.level.root`, `logging.level.com.mailSender`, `logging.level.com.mailSender.smtp`)
* [x] Sending configuration (`mail.batch-enabled`, `mail.dry-run`, `mail.html`, `mail.send-delay-ms`, test-send)

Avoid magic values such as:

```java
Thread.sleep(60000);
```

Use named configuration (`mail.send-delay-ms` / `MAIL_SEND_DELAY_MS`). Verified with `mvn -q test` (batch off / dry-run / mocked `JavaMailSender`; no live SMTP).

---

# M1-017 — Environment-specific configuration [x]

* [x] Spring profiles `development` (default) and `production`.
* [x] Production credentials are not in source control (env / gitignored local properties only).

Create support for:

```text
development
production
```

Do not include production credentials in source control. Verified with `mvn -q test` (no live SMTP).

---

# 14. Phase 6 — Logging

# M1-018 — Implement structured logging [x]

* [x] Application startup and shutdown (`ApplicationLifecycleLogger`)
* [x] Excel file loaded plus total / valid contacts / invalid contacts / duplicates
* [x] SMTP connection ready (host/port/tls/auth, never password) or skipped in dry-run
* [x] Campaign processing started; email sent successfully; email delivery failed

Log:

* Application startup
* Excel file loaded
* Number of rows
* Number of valid emails
* Number of invalid emails
* SMTP connection result
* Email send success
* Email send failure
* Application shutdown

NEVER log:

* SMTP password
* Authentication tokens
* Full sensitive contact data unnecessarily

Example:

```text
INFO  Excel file loaded: contacts.xlsx
INFO  Total rows: 500
INFO  Valid contacts: 472
INFO  Invalid contacts: 18
INFO  Campaign processing started
INFO  Email sent successfully
ERROR Email delivery failed
```

Verified with `mvn -q test` (no live SMTP).

---

# 15. Phase 7 — Error Handling

# M1-019 — Centralize exceptions [x]

* [x] `ExcelProcessingException` for Excel file/structure failures
* [x] `TemplateValidationException` for body file and placeholder/subject/body validation
* [x] `SmtpConfigurationException` for missing SMTP/from/sent-log/attachment/test-send settings
* [x] `EmailSendingException` for campaign/send/sent-log/attachment I/O; `SmtpSendException` extends it
* [x] No `InvalidContactException` — invalid/duplicate Excel rows are skipped, not thrown

Create meaningful custom exceptions where appropriate.

Examples:

```text
ExcelProcessingException
InvalidContactException
TemplateValidationException
EmailSendingException
SmtpConfigurationException
```

Do not create unnecessary custom exceptions. Verified with `mvn -q test` (no live SMTP).

---

# M1-020 — User-friendly error messages [x]

* [x] Operator messages use plain sentences (`Unable to process the Excel file because the Email column was not found.`).
* [x] Unexpected failures (including `NullPointerException`) are logged with a stack trace and rethrown as `EmailSendingException` without the exception class name in the user message.
* [x] SMTP classifier no longer appends raw provider text for configuration errors.

Replace technical errors where possible.

Bad:

```text
NullPointerException
```

Better:

```text
Unable to process the Excel file because the Email column was not found.
```

Keep technical details in logs. Verified with `mvn -q test` (no live SMTP).

---

# 16. Phase 8 — Testing

# M1-021 — Unit tests for Excel processing [x]

Test (in `ExcelReaderTest`, DisplayName per case):

* [x] Valid Excel
* [x] Empty Excel
* [x] Missing email column
* [x] Invalid emails
* [x] Duplicate emails
* [x] Empty rows
* [x] Multiple columns

Verified with `mvn -q test` (no live SMTP). Extra-column placeholders map to `Contact` keys.

---

# M1-022 — Unit tests for template rendering [x]

Test (in `TemplateRendererTest`; keys are case-insensitive):

```text
{{Name}}
{{Email}}
{{Company}}
```

Also test:

* [x] Missing field — omitted Excel column renders as empty
* [x] Unknown placeholder — `{{Unknown}}` renders as empty (validator still rejects it before send; that is M1-023)
* [x] Empty value
* [x] Multiple placeholders
* [x] Empty template

Verified with `mvn -q test` (no live SMTP).

---

# M1-023 — Unit tests for validation [x]

Test all validation rules.

* [x] Excel: missing file, non-`.xlsx`, empty sheet, empty row, invalid email, duplicate (normalized) email — `ExcelValidatorTest`
* [x] Template: empty/null subject and body, missing email/name keys, unknown placeholder, broken `{{` / stray `}}`, valid body — `TemplateValidatorTest`
* [x] SMTP preflight: username, password, and from required — `SmtpConfigurationTest` (`isReadyForSend`)

Target:

> At least 80% coverage for the newly refactored core business logic.

JaCoCo reports `com.mailSender.excel`, `template`, and `campaign` after `mvn test` (`target/site/jacoco/index.html`). Measured 2026-09-05 (no live SMTP):

```text
Instructions: 98% (14 missed of 1,161)
Branches:     84% (27 missed of 174)
Lines:        98% (3 missed of 285)
```

Do not manipulate coverage numbers artificially. Verified with `mvn -q test`.

---

# M1-024 — EmailSender tests [x]

Do not send real emails from unit tests.

Mock the SMTP/email provider dependency.

Test (`SmtpEmailSenderTest`, mocked `JavaMailSender`):

* [x] Successful send
* [x] Authentication failure
* [x] Connection failure
* [x] Invalid recipient
* [x] Timeout
* [x] Provider rejection

Verified with `mvn -q test` (no live SMTP). Dry-run coverage remains in `SmtpEmailSenderDryRunTest`.

---

# 17. Phase 9 — Integration Verification

# M1-025 — End-to-end local test [x]

Run:

```text
Excel
 ↓
Read
 ↓
Validate
 ↓
Create Contact
 ↓
Render Template
 ↓
Create EmailMessage
 ↓
Send Test Email
```

Verify the complete workflow.

Covered by `EndToEndLocalWorkflowTest`: real Excel + body files, `BatchMailRunner` test-send, first-row placeholders, To = `mail.test-send-to`. Transport is a mock `EmailSender` (no live SMTP). Verified with `mvn -q test`.

---

# M1-026 — Regression test [x]

Verify that the original use case still works:

```text
Excel
 ↓
Read email addresses
 ↓
Create fixed email
 ↓
SMTP
 ↓
Send
```

The refactoring must not remove existing functionality.

Covered by `OriginalExcelSmtpRegressionTest`: `BatchMailRunner` batch (test-send off) still mails **each** Excel contact with the shared subject/body template. Transport is a mock `EmailSender` (no live SMTP). Verified with `mvn -q test`.

---

# 18. Phase 10 — Documentation

# M1-027 — Developer documentation [x]

Create/update:

```text
README.md
docs/ARCHITECTURE.md
docs/PROJECT_ANALYSIS.md
docs/BASELINE.md
docs/TECHNICAL_DEBT.md
```

README must explain:

* [x] What the application does
* [x] Requirements
* [x] Installation
* [x] Configuration
* [x] How to run
* [x] Excel format
* [x] SMTP setup
* [x] How to send a test email
* [x] Troubleshooting

`docs/EXCEL_FORMAT.md` is **M1-028** (not this ID). Verified with `mvn -q test` (no live SMTP; docs-only).

---

# M1-028 — Excel format documentation [x]

Create:

`docs/EXCEL_FORMAT.md`

Document:

```text
Required columns
Optional columns
Supported placeholders
Invalid rows
Duplicate handling
Supported file types
```

Example:

```text
Name        Email              Company
Rahul       rahul@example.com  ABC
Priya       priya@example.com  XYZ
```

Linked from README. Verified with `mvn -q test` (no live SMTP).

---

# 19. Phase 11 — Code Quality

# M1-029 — Clean unused code [x]

After tests pass:

* [x] Remove unused imports (none leftover in production after this pass).
* [x] Remove unused methods / dead catch (`SmtpSendException` rethrow that nothing threw).
* [x] Remove dead code.
* [x] Remove duplicate code (`SmtpEmailSenderErrorTest`, duplicate empty-workbook Excel test).
* [x] Rename unclear variables (`rowIndex`; `contactForTestSend`).
* [x] Improve class/method naming (test-send helper only; no `MailBody` rewrite).

Do NOT perform unrelated rewrites. Verified with `mvn -q test` (no live SMTP).

---

# M1-030 — Dependency review [x]

Review dependencies. Full table: [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md).

| Name | Version | Purpose | Used? | Required? |
| --- | --- | --- | --- | --- |
| `spring-boot-starter` | 3.2.3 | CLI Spring context | Yes | Yes |
| `spring-boot-starter-mail` | 3.2.3 | SMTP / JavaMail | Yes | Yes |
| `spring-boot-starter-test` | 3.2.3 (test) | JUnit / Mockito | Yes | Yes (tests) |
| `poi-ooxml` | 5.2.3 | `.xlsx` | Yes | Yes |

Remove unnecessary dependencies only after verifying they are unused.

Do not upgrade major versions unnecessarily during this milestone.

No direct dependency was unused. JaCoCo plugin version pinned to **0.8.11** (Spring Boot 3.2 line; not a major upgrade). Verified with `mvn -q test` (no live SMTP).

---

# 20. Final Milestone Verification

# M1-031 — Full build [x]

Verified 2026-09-05 (no live SMTP; tests use mocked `JavaMailSender` / dry-run).

```bash
mvn clean verify
```

**Result:** `BUILD SUCCESS`. Surefire: **115** tests, **0** failures, **0** errors, **0** skipped. Follow-up `mvn -q test` also passed.

This ID is the Maven lifecycle (clean + compile + test + package/verify). The dedicated full-suite write-up is **M1-032**.

---

# M1-032 — Full test suite [x]

Verified 2026-09-05 with `mvn -q test` (no live SMTP; `JavaMailSender` mocked; dry-run paths do not send).

```text
Tests: PASS
Build: PASS (test compile + Surefire)
No compilation errors
No exposed credentials: test fixtures use placeholders only (e.g. `super-secret-app-password` is asserted *not* to appear in logs). Repo-wide secret scan is M1-033.
```

Surefire (`target/surefire-reports`): **115** tests, **0** failures, **0** errors, **0** skipped across **30** test classes.

---

# M1-033 — Security scan [x]

Searched the working tree (source, tests, committed properties, examples, README, `.gitignore`) for `password`, `secret`, `token`, `apikey`, and `smtp`. Confirmed **no real credentials** in current committed files. Verified 2026-09-05 with `mvn -q test` (no live SMTP). Repeatable checks live in `MailProfileFilesTest`.

```text
password — property/env names, operator copy, tests (placeholder `secret` / `super-secret-app-password` asserted not logged)
secret   — test fixtures only
token    — task/docs wording; no API tokens in source
apikey   — task wording only; no API keys in source
smtp     — host names (`smtp.gmail.com`), package `com.mailSender.smtp`, MAIL_SMTP_* flags
```

**`.gitignore`:** `application-local.properties`, `.env`, `sent-addresses.txt`. Neither `.env` nor `application-local.properties` is tracked (`git ls-files`).

**Environment:** `application.properties` binds `spring.mail.password=${MAIL_PASSWORD:${SMTP_PASSWORD:}}` (empty default). `development` / `production` profiles have no password keys. `.env.example` has empty `MAIL_PASSWORD=` / `SMTP_PASSWORD=`. Example local file uses placeholder `your-app-password`.

**Logs:** SMTP ready log is host/port/tls/auth only (`BatchMailRunner.logSmtpConnectionResult`). `SmtpConfiguration.toString()` uses `password=***`. Lifecycle logger does not log credentials.

**Exception messages:** Operator text names `MAIL_PASSWORD` as a setting to check; it does not include the password value. Classifier matches provider phrases such as “username and password not accepted” without echoing credentials.

**I1:** If an App Password was ever committed historically, revoke it in Google Account settings. Do not rewrite git history unless the user asks.

---

# M1-034 — Final architecture review [x]

Verified 2026-09-05 (imports inspected; `ArchitectureLayeringTest`; `mvn -q test`; no live SMTP). The CLI pipeline matches:

```text
Excel Layer
     ↓
Domain Model
     ↓
Template Layer
     ↓
Email Model
     ↓
EmailSender
     ↓
SMTP
```

| Layer | Types |
| --- | --- |
| Excel | `ExcelReader`, `ExcelValidator` |
| Domain | `Contact` |
| Template | `EmailTemplate`, `TemplateRenderer`, `TemplateValidator` |
| Email model | `EmailMessage` via `EmailComposer` |
| EmailSender | `smtp.EmailSender` |
| SMTP | `SmtpEmailSender` (dry-run skips `JavaMailSender`) |

**Circular dependencies:** none between excel / template / campaign / smtp / config. Edges are one-way down the pipeline (`template` → `Contact`; `campaign` → template + `Contact`; `smtp` → `EmailMessage`). `config` does not import those packages.

**Accepted leftover (not a layer cycle):** root `MailBodyAttachment` ↔ `smtp` (`SmtpEmailSender` uses the helper; the helper throws `EmailSendingException`). Campaign loop remains in `MailBody` above the packages. Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

# M1-035 — Prepare for Milestone 2 [x]

Created [docs/NEXT_MILESTONE.md](docs/NEXT_MILESTONE.md) (documentation only). Linked from README. **None** of the Milestone 2 items were implemented (no web API, Postgres, Flyway, auth, persistence, or background workers). Verified 2026-09-05 with `mvn -q test` (no live SMTP).

Documented requirements:

```text
Spring Boot API
PostgreSQL
Flyway
REST endpoints
Authentication
Campaign persistence
Contact persistence
Template persistence
SMTP configuration persistence
Background email processing
```

---

# 21. Definition of Done

Milestone 1 is complete ONLY when all of the following are true:

* [x] Existing application behavior is documented.
* [x] Existing application can still run.
* [x] Excel processing is separated.
* [x] Contact model exists.
* [x] Excel validation exists.
* [x] Template rendering is separated.
* [x] Email message model exists.
* [x] SMTP configuration is separated.
* [x] EmailSender abstraction exists.
* [x] Credentials are externalized.
* [x] Logging is implemented/improved.
* [x] Error handling is improved.
* [x] Unit tests exist.
* [x] Core refactored logic has approximately 80%+ test coverage.
* [x] End-to-end workflow has been verified.
* [x] Documentation has been updated.
* [x] No real credentials exist in Git.
* [x] Build passes.
* [x] Tests pass.
* [x] No unrelated features were implemented.

Checked 2026-09-05 after M1-001–M1-035. Runtime remains the Excel → SMTP CLI (dry-run / mocked mail in tests). SaaS items in [docs/NEXT_MILESTONE.md](docs/NEXT_MILESTONE.md) were documented, not built.

---

# 22. Agent Final Report

**Scope:** ExcelMail Pro Milestone 1, tasks **M1-001 through M1-035**, closed 2026-09-05. No Milestone 2 / SaaS code.

## Completed

**Discovery:** M1-001–M1-003 (`docs/PROJECT_ANALYSIS.md`, `BASELINE.md`, `TECHNICAL_DEBT.md`).

**Architecture & extract:** M1-004–M1-015 — `excel`, `template`, `smtp`, `campaign`, `config`; `Contact`; `EmailSender` / `SmtpEmailSender`; `EmailMessage` / `EmailComposer`; credentials off git; test-send.

**Config, logs, errors:** M1-016–M1-020 — `MailAppProperties`, profiles, structured logs, typed exceptions, operator messages.

**Tests:** M1-021–M1-026 — Excel, template, validation/JaCoCo, mocked SMTP, local e2e, batch regression.

**Docs & hygiene:** M1-027–M1-030 — README/`docs/`, `EXCEL_FORMAT.md`, unused-code cleanup, `DEPENDENCIES.md`.

**Verify:** M1-031 `mvn clean verify`; M1-032 full suite; M1-033 secret scan; M1-034 layering; M1-035 `docs/NEXT_MILESTONE.md` only.

## Files Changed

Representative Milestone 1 artifacts (not every test file):

* Application: `MailSenderApplication`, `BatchMailRunner`, `MailBody`, `MailBodyAttachment`, `SentAddressLog`
* `excel/`, `template/`, `campaign/`, `smtp/`, `config/`
* `src/main/resources/application*.properties`, `.env.example`, `application-local.properties.example`
* `README.md`, `docs/ARCHITECTURE.md`, `EXCEL_FORMAT.md`, `DEPENDENCIES.md`, `NEXT_MILESTONE.md`, `DEVELOPMENT_TASKS.md`
* Tests under `src/test/java/com/mailSender/` (including `ArchitectureLayeringTest`, `MailProfileFilesTest`)

## Architecture Changes

**Before:** Single-package CLI; Excel, template, and SMTP mixed; credentials at risk of being hardcoded; weak tests.

**After:** Pipeline Excel → `Contact` → template → `EmailMessage` → `EmailSender` → `SmtpEmailSender` (dry-run skips JavaMail). `mail.*` / `spring.mail.*` from env or gitignored local file. Default batch off, dry-run on. Campaign loop still in `MailBody`; attachment helper still in the root package.

## Tests

Verified 2026-09-05 with `mvn -q test` (no live SMTP).

```text
Total tests: 122
Passed: 122
Failed: 0
Skipped: 0
Coverage: excel+template+campaign JaCoCo 98% instructions / 84% branches / 98% lines (`target/site/jacoco` after `mvn test`; not whole-app coverage)
```

`mvn clean verify` passed (M1-031).

## Security

```text
Credentials removed: YES (no App Passwords in committed properties/profiles)
Secrets committed: NO (application-local.properties / .env remain gitignored; examples use placeholders)
Environment configuration: YES (MAIL_* / spring.mail.* / mail.*)
```

I1 (rotate any previously leaked Gmail App Password) remains a Google Account action for the user.

## Known Issues

* `MailBody` not renamed; `MailBodyAttachment` / `SentAddressLog` in root; smtp ↔ attachment helper coupling (M1-034).
* Email validity is still `contains("@")`; subject is not templated; Gmail-oriented defaults; no Maven Wrapper.
* Production profile keeps batch off / dry-run on unless overridden (intentional).
* Git history may still contain old secrets; do not rewrite history unless asked.

## Milestone 2 Readiness

```text
Is the project ready for Spring Boot/SaaS development?
YES (as a CLI core to wrap — see docs/NEXT_MILESTONE.md)

If NO:
(not applicable)

Caveat: the running app is still non-web. APIs, auth, PostgreSQL, Flyway, persistence, and workers are not implemented. Do not start payments or tracking pixels in the same pass as the first API.
```

---

# 23. Important Agent Constraint

DO NOT mark a task `[x]` merely because code was written.

A task can be marked `[x]` only after:

```text
Implementation
     ↓
Compilation
     ↓
Tests
     ↓
Verification
     ↓
Documentation
```

If something cannot be verified, mark it:

```text
[!]
```

and explain why.

---

# 24. Milestone Completion

At the end of Milestone 1, the project should have evolved from:

```text
┌─────────────────────────────┐
│ Existing SMTP Application   │
│                             │
│ Excel + SMTP + Logic        │
│ all mixed together          │
└─────────────────────────────┘
```

into:

```text
┌──────────────────────────────────┐
│        ExcelMail Pro Core        │
│                                  │
│ Excel Processing                 │
│        ↓                         │
│ Contact Model                    │
│        ↓                         │
│ Template Renderer                │
│        ↓                         │
│ Email Message                    │
│        ↓                         │
│ EmailSender                      │
│        ↓                         │
│ SMTP Implementation              │
└──────────────────────────────────┘
```

This core will become the foundation for the SaaS architecture in Milestone 2.

---

## Suggested order

Work Phase 1 discovery first (M1-001 → M1-003), then architecture (M1-004), then implementation phases in order (M1-005 through M1-035). One task per pass unless tightly coupled (e.g. M1-005 + M1-006).

Agents: start with [AGENTS.md](AGENTS.md), then follow [AGENT_WORKFLOW.md](AGENT_WORKFLOW.md). Project rules: `.cursor/rules/`.
