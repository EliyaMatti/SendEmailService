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

# M1-016 — Centralize configuration

Remove scattered configuration values.

Centralize:

* SMTP settings
* Input file location
* Logging configuration
* Sending configuration

Avoid magic values such as:

```java
Thread.sleep(60000);
```

Use named configuration:

```text
sending.delay
```

---

# M1-017 — Environment-specific configuration

Create support for:

```text
development
production
```

Do not include production credentials in source control.

---

# 14. Phase 6 — Logging

# M1-018 — Implement structured logging

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

---

# 15. Phase 7 — Error Handling

# M1-019 — Centralize exceptions

Create meaningful custom exceptions where appropriate.

Examples:

```text
ExcelProcessingException
InvalidContactException
TemplateValidationException
EmailSendingException
SmtpConfigurationException
```

Do not create unnecessary custom exceptions.

---

# M1-020 — User-friendly error messages

Replace technical errors where possible.

Bad:

```text
NullPointerException
```

Better:

```text
Unable to process the Excel file because the Email column was not found.
```

Keep technical details in logs.

---

# 16. Phase 8 — Testing

# M1-021 — Unit tests for Excel processing

Test:

* Valid Excel
* Empty Excel
* Missing email column
* Invalid emails
* Duplicate emails
* Empty rows
* Multiple columns

---

# M1-022 — Unit tests for template rendering

Test:

```text
{{Name}}
{{Email}}
{{Company}}
```

Also test:

* Missing field
* Unknown placeholder
* Empty value
* Multiple placeholders
* Empty template

---

# M1-023 — Unit tests for validation

Test all validation rules.

Target:

> At least 80% coverage for the newly refactored core business logic.

Do not manipulate coverage numbers artificially.

---

# M1-024 — EmailSender tests

Do not send real emails from unit tests.

Mock the SMTP/email provider dependency.

Test:

* Successful send
* Authentication failure
* Connection failure
* Invalid recipient
* Timeout
* Provider rejection

---

# 17. Phase 9 — Integration Verification

# M1-025 — End-to-end local test

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

---

# M1-026 — Regression test

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

---

# 18. Phase 10 — Documentation

# M1-027 — Developer documentation

Create/update:

```text
README.md
docs/ARCHITECTURE.md
docs/PROJECT_ANALYSIS.md
docs/BASELINE.md
docs/TECHNICAL_DEBT.md
```

README must explain:

* What the application does
* Requirements
* Installation
* Configuration
* How to run
* Excel format
* SMTP setup
* How to send a test email
* Troubleshooting

---

# M1-028 — Excel format documentation

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

---

# 19. Phase 11 — Code Quality

# M1-029 — Clean unused code

After tests pass:

* Remove unused imports.
* Remove unused methods.
* Remove dead code.
* Remove duplicate code.
* Rename unclear variables.
* Improve class/method naming.

Do NOT perform unrelated rewrites.

---

# M1-030 — Dependency review

Review dependencies.

For each dependency:

```text
Name
Version
Purpose
Used?
Required?
```

Remove unnecessary dependencies only after verifying they are unused.

Do not upgrade major versions unnecessarily during this milestone.

---

# 20. Final Milestone Verification

# M1-031 — Full build

Run the project's correct build command.

Examples:

```bash
mvn clean verify
```

or the project's equivalent.

The build must pass.

---

# M1-032 — Full test suite

Run all tests.

Expected:

```text
Tests: PASS
Build: PASS
No compilation errors
No exposed credentials
```

---

# M1-033 — Security scan

Search the repository for:

```text
password
secret
token
apikey
smtp
```

Confirm no real credentials are committed.

Check:

* `.gitignore`
* environment configuration
* logs
* exception messages

---

# M1-034 — Final architecture review

Verify that the architecture follows:

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

No unnecessary circular dependencies.

---

# M1-035 — Prepare for Milestone 2

Create:

`docs/NEXT_MILESTONE.md`

Document what will be required for the next phase:

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

Do NOT implement these in Milestone 1.

---

# 21. Definition of Done

Milestone 1 is complete ONLY when all of the following are true:

* [ ] Existing application behavior is documented.
* [ ] Existing application can still run.
* [ ] Excel processing is separated.
* [ ] Contact model exists.
* [ ] Excel validation exists.
* [ ] Template rendering is separated.
* [ ] Email message model exists.
* [ ] SMTP configuration is separated.
* [ ] EmailSender abstraction exists.
* [ ] Credentials are externalized.
* [ ] Logging is implemented/improved.
* [ ] Error handling is improved.
* [ ] Unit tests exist.
* [ ] Core refactored logic has approximately 80%+ test coverage.
* [ ] End-to-end workflow has been verified.
* [ ] Documentation has been updated.
* [ ] No real credentials exist in Git.
* [ ] Build passes.
* [ ] Tests pass.
* [ ] No unrelated features were implemented.

---

# 22. Agent Final Report

When all tasks are complete, the agent MUST produce a final report containing:

## Completed

List every completed task.

## Files Changed

List all created/modified/deleted files.

## Architecture Changes

Explain the before/after architecture.

## Tests

Report:

```text
Total tests
Passed
Failed
Skipped
Coverage
```

## Security

Confirm:

```text
Credentials removed: YES/NO
Secrets committed: YES/NO
Environment configuration: YES/NO
```

## Known Issues

List remaining technical debt.

## Milestone 2 Readiness

Answer:

```text
Is the project ready for Spring Boot/SaaS development?
YES / NO

If NO:
List blockers.
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
