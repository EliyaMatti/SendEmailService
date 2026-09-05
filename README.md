# SendEmailService

A Spring Boot **command-line** app by default (no HTTP). On startup it can read a recipient list from Excel, fill a UTF-8 body template with `{{placeholders}}`, optionally attach a file, and send mail over SMTP.

It is meant for one batch per process: enable the batch, run once, then the JVM exits. It is not a long-running mail server.

An optional **`api` profile** (`SPRING_PROFILES_ACTIVE=api`) starts a servlet container for Milestone 2 REST work. Default `mvn spring-boot:run` stays `WebApplicationType.NONE` (no Tomcat). The batch runner is not loaded on `api`, so HTTP startup does not send mail.

## What it does

1. Reads recipients from an `.xlsx` file (`email`, `name`, and any extra columns).
2. Loads a body template and replaces `{{key}}` with that row’s values (`{{name}}`, `{{email}}`, `{{company}}`, …).
3. **Dry-run (default):** logs each To address and personalized body; does not call SMTP.
4. **Real send:** sends via SMTP (Gmail or other), one message per recipient, with an optional attachment.
5. After a successful send, appends the address to a sent-log file so later runs skip that person.
6. Logs a summary (`sent` / `failed` / `skipped`). One SMTP error does not stop the rest of the list. If any send failed, the process exits non-zero.

Startup does **not** send mail unless you turn the batch on **and** turn dry-run off.

## Prerequisites

- Java 17+
- Maven
- An Excel list and a body text (or HTML) file
- For real sends: SMTP credentials (for Gmail, an **App Password**, not your account password)

Do **not** put SMTP passwords in source control. Any Gmail App Password that was previously hardcoded in this repo should be **revoked and rotated** in Google Account settings; it may still exist in git history.

## Installation

1. Clone the repository and install **Java 17+** and **Maven**.
2. From the repo root, compile and test (no live SMTP): `mvn test`. Full Maven lifecycle (clean + tests + package/verify): `mvn clean verify`.
3. Copy `src/main/resources/application-local.properties.example` to gitignored `src/main/resources/application-local.properties` (or set environment variables). Leave `MAIL_PASSWORD` empty until you need a real send.
4. Prepare an `.xlsx` recipient file and a UTF-8 body template (the repo does not ship sample lists).

More design notes: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/PROJECT_ANALYSIS.md](docs/PROJECT_ANALYSIS.md). Excel columns and placeholders: [docs/EXCEL_FORMAT.md](docs/EXCEL_FORMAT.md). Dependencies: [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md). After Milestone 1: [docs/NEXT_MILESTONE.md](docs/NEXT_MILESTONE.md). Discovery snapshots: [docs/BASELINE.md](docs/BASELINE.md), [docs/TECHNICAL_DEBT.md](docs/TECHNICAL_DEBT.md).

## How to use

### 1. Prepare the Excel file

See [docs/EXCEL_FORMAT.md](docs/EXCEL_FORMAT.md) for required/optional columns, placeholders, invalid rows, duplicates, and file types.

First row should be headers that include **email** and **name** (aliases such as `e-mail` / `full_name` work). Extra headers become placeholders: non-word characters are stripped and the name is lowercased (`Company` → `{{company}}`).

If the first row does not look like headers, column A is email and column B is name.

Blank rows and cells without `@` are skipped (counted as invalid). Duplicate addresses in the same file (case-insensitive) are skipped after the first valid row. Formula and numeric cells are read with Apache POI `DataFormatter`. Only `.xlsx` is supported.

After a successful read the app logs a validation summary:

```text
Total rows: 500
Valid: 472
Invalid: 18
Duplicates: 10
```

The same counts are also logged (`Valid contacts` / `Invalid contacts`). A missing file, empty workbook, or non-`.xlsx` path fails before any mail is sent.

### 2. Write the body template

Save a UTF-8 text file. Placeholders look like `{{key}}` (letter case does not matter: `{{Name}}` and `{{name}}` are the same). Built-in keys are `{{name}}` and `{{email}}`. Extra Excel columns add more keys.

Before sending, the template is checked: empty subject, empty body, broken `{{…}}` syntax, or a placeholder that is not a column in the Excel file all fail with `Template validation failed` (no SMTP). Example:

```text
Template validation failed:
Placeholder {{Company}} does not exist in the imported data.
```

```
Hi {{name}},

This message was sent to {{email}} at {{company}}.
```

For HTML bodies, write HTML in the file and set `mail.html=true` (or `MAIL_HTML=true`).

### 3. Optional attachment

Set `mail.attachment-path` to a readable file (for example a PDF). If the path is set but the file is missing, a real send fails **before** any message goes out. Leave the path empty to send without an attachment.

### 4. SMTP setup and paths

Copy the example file and fill in values. `application-local.properties` is gitignored.

```
copy src\main\resources\application-local.properties.example src\main\resources\application-local.properties
```

Set at least:

- `spring.mail.username` / `spring.mail.password`
- `mail.from`
- `mail.excel-file-path`
- `mail.body-file-path`

You can use environment variables instead of that file; see [Configuration](#configuration). A placeholder list is in `.env.example` (copy to gitignored `.env` only as a reminder — Spring Boot does not load `.env` automatically).

### 5. Preview (no SMTP)

Keep dry-run on. Enable the batch, then run:

```
mvn spring-boot:run
```

With env vars (Unix):

```
MAIL_BATCH_ENABLED=true MAIL_DRY_RUN=true mvn spring-boot:run
```

PowerShell:

```
$env:MAIL_BATCH_ENABLED="true"; $env:MAIL_DRY_RUN="true"; mvn spring-boot:run
```

Or set `mail.batch-enabled=true` and `mail.dry-run=true` in `application-local.properties`.

You should see each To address and the filled-in body. Nothing is sent, and the sent-log file is not written.

### 5b. Send a test email (one address, not the Excel list)

This uses the configured SMTP account (unless dry-run is on), renders the body template, and sends **one** message to `mail.test-send-to`. It does **not** send to everyone in the Excel file, even if `mail.batch-enabled` is true.

If `mail.excel-file-path` is set, placeholders (`{{name}}`, extra columns) come from the **first valid row**, but **To** is always the test address. If Excel is unset, the template is filled with the test address and name `Test`.

Keep `mail.batch-enabled=false` (or leave it on; test-send still wins). Set:

- `mail.test-send-enabled=true` / `MAIL_TEST_SEND_ENABLED=true`
- `mail.test-send-to=you@example.com` / `MAIL_TEST_SEND_TO`
- `mail.body-file-path` (required)
- For a real SMTP test: `mail.dry-run=false` plus username, password, and `mail.from`

Unix:

```
MAIL_TEST_SEND_ENABLED=true MAIL_TEST_SEND_TO=you@example.com MAIL_BODY_FILE_PATH=body.txt MAIL_DRY_RUN=true mvn spring-boot:run
```

PowerShell:

```
$env:MAIL_TEST_SEND_ENABLED="true"; $env:MAIL_TEST_SEND_TO="you@example.com"; $env:MAIL_BODY_FILE_PATH="body.txt"; $env:MAIL_DRY_RUN="true"; mvn spring-boot:run
```

Success logs `Email sent successfully`. A send failure logs `Email delivery failed` with a short reason (not a stack trace). The sent-address log is **not** updated.

### 6. Send for real

Use a test inbox first. You need SMTP username, password, from-address, and a non-blank sent-log path (default `sent-addresses.txt`, gitignored).

Set `mail.batch-enabled=true` and `mail.dry-run=false`, then run `mvn spring-boot:run`.

Unix:

```
MAIL_BATCH_ENABLED=true MAIL_DRY_RUN=false MAIL_SENT_LOG_PATH=sent-addresses.txt mvn spring-boot:run
```

PowerShell:

```
$env:MAIL_BATCH_ENABLED="true"; $env:MAIL_DRY_RUN="false"; $env:MAIL_SENT_LOG_PATH="sent-addresses.txt"; mvn spring-boot:run
```

There is a pause of `mail.send-delay-ms` (default 1000) between real send attempts (not after the last, and not in dry-run).

A failed send logs `Email delivery failed` with a short reason (authentication, connection, timeout, invalid recipient, SMTP rejection, or configuration) instead of a raw stack trace. One failure does not stop later recipients. Enable debug logging on `com.mailSender.smtp` if you need the underlying exception.

### Logging

The app logs: application startup and shutdown; Excel file loaded plus total / valid / invalid / duplicate counts; SMTP connection ready (host, port, TLS, auth — **never** the password) or SMTP skipped in dry-run; campaign processing started; email sent successfully; email delivery failed; batch summary.

Do not expect passwords, tokens, or extra Excel columns in logs. To-addresses appear for skip/dry-run/invalid-row diagnostics only.

Operator-facing errors are plain sentences (for example `Unable to process the Excel file because the Email column was not found.`). Exception class names and stack traces stay in the log.

### Sent log and re-runs

Successful To-addresses are appended to `mail.sent-log-path` and skipped on later runs. Dry-run does not write that log, but it still **skips addresses already recorded** there, so a re-run preview hides those rows.

If the recipient list is empty after skipping blanks/invalid emails, the job logs a warning and exits 0.

## Configuration

Named settings live in `src/main/resources/application.properties` (grouped: SMTP, files, sending, logging). Campaign keys bind to `MailAppProperties`; SMTP host, port, credentials, and STARTTLS bind to `SmtpConfiguration`. Inter-send pause is `mail.send-delay-ms` (env `MAIL_SEND_DELAY_MS`) — there is no hardcoded `Thread.sleep(60000)`. Logging levels are `logging.level.root` and `logging.level.com.mailSender`.

The default Spring profile is **`development`**. Use **`production`** for a deploy-shaped config that still contains **no passwords**. Use **`api`** to start the servlet (Milestone 2); combine as `development,api` if needed. Activate with `SPRING_PROFILES_ACTIVE` or `--spring.profiles.active`. Put real credentials only in the environment or gitignored `application-local.properties`. CLI and API profiles keep batch off and dry-run on unless you override those flags.

### Environment variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring profile (`development`, `production`, and/or `api`) | `development` (via `spring.profiles.default`) |
| `MAIL_USERNAME` | SMTP username | empty |
| `MAIL_PASSWORD` | SMTP password (Gmail App Password) | empty |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_FROM` | From address | SMTP username |
| `MAIL_FROM_NAME` | Optional From display name (`mail.from-name`) | empty |
| `MAIL_SMTP_AUTH` | SMTP AUTH (`spring.mail.properties.mail.smtp.auth`) | `true` |
| `MAIL_SMTP_STARTTLS` | STARTTLS (`spring.mail.properties.mail.smtp.starttls.enable`) | `true` |
| `SMTP_HOST` | Alias for `MAIL_HOST` if `MAIL_HOST` is unset | `smtp.gmail.com` |
| `SMTP_PORT` | Alias for `MAIL_PORT` if `MAIL_PORT` is unset | `587` |
| `SMTP_USERNAME` | Alias for `MAIL_USERNAME` if `MAIL_USERNAME` is unset | empty |
| `SMTP_PASSWORD` | Alias for `MAIL_PASSWORD` if `MAIL_PASSWORD` is unset | empty |
| `SMTP_FROM_EMAIL` | Alias for `MAIL_FROM` if `MAIL_FROM` is unset | SMTP username |
| `MAIL_SUBJECT` | Email subject | configured default |
| `MAIL_EXCEL_FILE_PATH` | Recipients Excel path | empty |
| `MAIL_BODY_FILE_PATH` | Body template text file | empty |
| `MAIL_ATTACHMENT_PATH` | Optional attachment | empty |
| `MAIL_BATCH_ENABLED` | Run send-on-startup batch | `false` |
| `MAIL_TEST_SEND_ENABLED` | Send one test message instead of the Excel list | `false` |
| `MAIL_TEST_SEND_TO` | Test-send recipient | empty |
| `MAIL_DRY_RUN` | Print To + body; skip SMTP | `true` |
| `MAIL_HTML` | Send body as HTML | `false` |
| `MAIL_SENT_LOG_PATH` | File of already-sent addresses | `sent-addresses.txt` |
| `MAIL_SEND_DELAY_MS` | Delay between real sends (`mail.send-delay-ms`; not a hardcoded sleep) | `1000` |
| `LOGGING_LEVEL_ROOT` | Root log level | `INFO` |
| `LOGGING_LEVEL_MAILSENDER` | `com.mailSender` log level | `INFO` |
| `LOGGING_LEVEL_SMTP` | `com.mailSender.smtp` log level | `INFO` |
| `DB_HOST` | PostgreSQL host (`api` profile) | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | PostgreSQL database name | `excelmail` |
| `DB_USERNAME` | PostgreSQL username | `excelmail` |
| `DB_PASSWORD` | PostgreSQL password (never commit) | empty |
| `APP_JWT_SECRET` | JWT HMAC secret (`api` profile, ≥32 chars) | empty |
| `APP_ENCRYPTION_KEY` | Base64 AES key for SMTP passwords (`api` profile, 16 or 32 bytes) | empty |
| `APP_JWT_EXPIRATION_MS` | JWT lifetime | `86400000` |
| `EXCELMAIL_WORKER_ENABLED` | Campaign poller on `api` profile | `true` (tests: `false`) |

CLI batch instructions stay in this README. HTTP API: [docs/API.md](docs/API.md), database: [docs/DATABASE.md](docs/DATABASE.md), security: [docs/SECURITY.md](docs/SECURITY.md).

### API mode (`api` profile)

```
SPRING_PROFILES_ACTIVE=api
DB_PASSWORD=... APP_JWT_SECRET=... APP_ENCRYPTION_KEY=...
mvn spring-boot:run
```

Default `mvn spring-boot:run` stays CLI (`WebApplicationType.NONE`) and does **not** need PostgreSQL. The `api` profile starts a servlet, runs Flyway, and does **not** load `BatchMailRunner`. Campaign sending is done by `CampaignWorker`, not on HTTP threads. Keep `MAIL_DRY_RUN=true` unless you intend real SMTP. Automated tests mock `JavaMailSender` / `EmailSender`.

OpenAPI: `/v3/api-docs` and `/swagger-ui.html`. Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics` (no env dump, mail health disabled).

Failures use typed runtime exceptions (`ExcelProcessingException`, `TemplateValidationException`, `SmtpConfigurationException`, `EmailSendingException`) with the same operator messages as before. Invalid Excel rows are skipped rather than thrown (`InvalidContactException` is not used).

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Batch never runs | `mail.batch-enabled` / `MAIL_BATCH_ENABLED` is false (default). Test-send, if enabled, runs instead of the Excel list. |
| Nothing is mailed | `mail.dry-run` defaults to **true**. Set `MAIL_DRY_RUN=false` only when you intend SMTP. |
| `Unable to process the Excel file because it was not found` | `MAIL_EXCEL_FILE_PATH` points at a real `.xlsx` file. |
| `… not a .xlsx workbook` | CSV/XLS are not supported. |
| `… Email column was not found` / `Name column was not found` | Header row must include email and name (see Excel section above). |
| `Template validation failed` | Subject/body non-empty; placeholders must match Excel columns; `{{` / `}}` must be well formed. |
| `Unable to read the email body file` | `MAIL_BODY_FILE_PATH` exists and is readable UTF-8. |
| `SMTP username, password, and from address are required` | Real send needs `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM`. Gmail: App Password, not account password. |
| `mail.sent-log-path is not set` | Real **batch** send needs a sent-log path (default `sent-addresses.txt`). |
| `attachment file could not be read` | `MAIL_ATTACHMENT_PATH` is set but the file is missing. Clear the path to send without an attachment. |
| Authentication failed | Rotate App Password; enable SMTP AUTH / STARTTLS. Never put the password in git. |
| Recipients skipped | Already listed in the sent-log, duplicate in the sheet, blank/`@`-less email, or empty row. |

Stack traces stay in the log. Operator messages are the short sentences above. Do not use live SMTP to debug in this project’s agent workflow; use dry-run or `mvn test`.
