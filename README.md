# SendEmailService

A Spring Boot **command-line** app (no HTTP API). On startup it can read a recipient list from Excel, fill a UTF-8 body template with `{{placeholders}}`, optionally attach a file, and send mail over SMTP.

It is meant for one batch per process: enable the batch, run once, then the JVM exits. It is not a long-running mail server.

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

## How to use

### 1. Prepare the Excel file

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

A missing file, empty workbook, or non-`.xlsx` path fails before any mail is sent.

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

### 4. Configure credentials and paths

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

### Sent log and re-runs

Successful To-addresses are appended to `mail.sent-log-path` and skipped on later runs. Dry-run does not write that log, but it still **skips addresses already recorded** there, so a re-run preview hides those rows.

If the recipient list is empty after skipping blanks/invalid emails, the job logs a warning and exits 0.

## Configuration

### Environment variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `MAIL_USERNAME` | SMTP username | empty |
| `MAIL_PASSWORD` | SMTP password (Gmail App Password) | empty |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_FROM` | From address | SMTP username |
| `MAIL_FROM_NAME` | Optional From display name (`mail.from-name`; SMTP config only) | empty |
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
| `MAIL_DRY_RUN` | Print To + body; skip SMTP | `true` |
| `MAIL_HTML` | Send body as HTML | `false` |
| `MAIL_SENT_LOG_PATH` | File of already-sent addresses | `sent-addresses.txt` |
| `MAIL_SEND_DELAY_MS` | Delay between real sends (ms) | `1000` |

Defaults in `application.properties` match this table. Real SMTP runs only when the batch is enabled **and** dry-run is off. Host, port, username, password, from address, from name, and STARTTLS are bound into `SmtpConfiguration`; the password is never written to logs.
