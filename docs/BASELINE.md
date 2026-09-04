# Baseline (M1-002)

Record of how the Excel → SMTP command-line app behaves **today**, before Milestone 1 architecture refactors. No production SMTP was used to produce this document.

Verification on 2026-09-04:

- `mvn -q test` — pass (JUnit; `JavaMailSender` mocked; context tests force `mail.batch-enabled=false`).
- `MAIL_BATCH_ENABLED=false` `mvn spring-boot:run` — process starts, logs batch skipped, JVM exits 0.
- Dry-run with temp `.xlsx` + UTF-8 body (`MAIL_BATCH_ENABLED=true`, `MAIL_DRY_RUN=true`, paths to files under `%TEMP%\mail-m1-002`) — logs `DRY-RUN To:` and personalized body; no SMTP.

The repo does **not** ship sample Excel or body files. Recipients and templates are always supplied by the operator.

---

## How the application starts

1. `com.mailSender.MailSenderApplication.main` builds a `SpringApplication`, sets `WebApplicationType.NONE` (no HTTP server), and runs.
2. Spring loads `src/main/resources/application.properties`, optionally imports classpath `application-local.properties` (gitignored), and binds `mail.*` onto `MailAppProperties`.
3. `BatchMailRunner` (`CommandLineRunner`) runs once after the context is up, then the JVM exits.

There is no REST API, scheduler, or long-lived worker. One process = at most one batch.

Runtime observed: Java 21.0.10. `pom.xml` compiles with `java.version` 17.

---

## Required input files

| Input | Required when batch is on | Notes |
| --- | --- | --- |
| Recipients Excel | Yes | Path `mail.excel-file-path` / `MAIL_EXCEL_FILE_PATH`. `.xlsx` only (`XSSFWorkbook`). First sheet only. |
| Body template | Yes | Path `mail.body-file-path` / `MAIL_BODY_FILE_PATH`. UTF-8 text (or HTML if `mail.html=true`). |
| Attachment | No | `mail.attachment-path`. Empty = no attachment. |

If batch is enabled but Excel **or** body path is blank, the runner logs a skip (config not set) and exits 0. If a path is set but the file is missing/unreadable, the process fails with `IllegalStateException` (Excel: `Cannot read Excel file: …`; body: `Cannot read body file: …`).

---

## Required configuration

Committed defaults in `application.properties` (git `HEAD`):

| Key | Env | HEAD default |
| --- | --- | --- |
| `spring.mail.host` | `MAIL_HOST` | `smtp.gmail.com` |
| `spring.mail.port` | `MAIL_PORT` | `587` |
| `spring.mail.username` | `MAIL_USERNAME` | empty |
| `spring.mail.password` | `MAIL_PASSWORD` | empty |
| `spring.mail.properties.mail.smtp.auth` | — | `true` |
| `spring.mail.properties.mail.smtp.starttls.enable` | — | `true` |
| `mail.from` | `MAIL_FROM` | SMTP username |
| `mail.subject` | `MAIL_SUBJECT` | `Java developer Application – Eliya` |
| `mail.excel-file-path` | `MAIL_EXCEL_FILE_PATH` | empty |
| `mail.body-file-path` | `MAIL_BODY_FILE_PATH` | empty |
| `mail.attachment-path` | `MAIL_ATTACHMENT_PATH` | empty |
| `mail.batch-enabled` | `MAIL_BATCH_ENABLED` | **`false`** |
| `mail.dry-run` | `MAIL_DRY_RUN` | **`true`** |
| `mail.html` | `MAIL_HTML` | `false` |
| `mail.sent-log-path` | `MAIL_SENT_LOG_PATH` | `sent-addresses.txt` |
| `mail.send-delay-ms` | `MAIL_SEND_DELAY_MS` | `1000` |

`MailAppProperties` Java field defaults match the safe pair: `batchEnabled=false`, `dryRun=true`. README and `application-local.properties.example` match HEAD.

**Working-tree note:** an uncommitted edit to `application.properties` currently sets `MAIL_BATCH_ENABLED` default `true` and `MAIL_DRY_RUN` default `false`, and replaces the en-dash in the subject with spaces. That is **not** the committed baseline. Combined with a gitignored `application-local.properties` (present on this machine; contents not recorded here), a naive `mvn spring-boot:run` can attempt real SMTP. Always override with `MAIL_DRY_RUN=true` (or batch off) during development.

Copy `src/main/resources/application-local.properties.example` → `application-local.properties` for local secrets. Do not commit that file.

---

## Excel format

- File type: Office Open XML **`.xlsx`**. A non-xlsx file at that path fails: `Cannot read Excel file`.
- Sheet: index 0.
- Cells: Apache POI `DataFormatter` + formula evaluator (formulas and numeric cells become trimmed strings).

**Header row** (preferred): first row contains both an email header and a name header (case-insensitive; non-word characters stripped). Email aliases: `email`, `e_mail`, `mail`. Name aliases: `name`, `fullname`, `full_name`. Extra headers become placeholder keys (`Company` → `company` → `{{company}}`).

If the first row has an email-like header but **no** name header, and column A is not an address containing `@`, the reader throws: `Excel header row must include email and name columns`.

**No headers:** if the first row is not recognized as a full header pair, column A is email and column B is name (even if B looks like the word `Name`). Extra columns are not mapped in this mode.

**Row handling:** null rows are ignored. Blank email → skip + warn (`Skipping row N: blank email`). Email without `@` → skip + warn. Remaining rows become `EmailRecipient` (email, name, extra placeholders). Duplicate addresses in the same file are **not** rejected at read time; the send loop skips a second send after a successful first (case-insensitive via `SentAddressLog.normalize`).

---

## Email format

- **To:** recipient email from Excel.
- **From:** `mail.from`.
- **Subject:** `mail.subject` (same subject for every recipient; not templated).
- **Body:** UTF-8 file; `{{word}}` placeholders replaced from `EmailRecipient.getPlaceholders()` (`email`, `name`, plus extra columns). Unknown placeholders become empty strings. `$` and `\` in names are treated as literals.
- **HTML:** if `mail.html=true`, `MimeMessageHelper.setText(body, true)`; otherwise plain text.
- **Attachment:** optional file name on the MIME message. Dry-run does not attach or open SMTP.

There is no per-recipient subject, CC/BCC, or reply-to field.

---

## SMTP configuration

Used only when `mail.batch-enabled=true` **and** `mail.dry-run=false`.

Before reading Excel, `BatchMailRunner` then requires:

- Non-blank `spring.mail.username`, `spring.mail.password`, and `mail.from`.
- Non-blank `mail.sent-log-path`.
- If attachment path is set: file exists and is readable (fail **before** the send loop).

Gmail-oriented defaults: host `smtp.gmail.com`, port 587, auth + STARTTLS. Username/password are expected to be an address + App Password, not hardcoded in source.

`EmailService.sendEmail` builds a `MimeMessage` via `JavaMailSender` and wraps failures as `RuntimeException("Failed to send email to " + to, e)`.

---

## Expected output

### Batch disabled (default HEAD)

```text
Mail batch skipped: mail.batch-enabled is false (set MAIL_BATCH_ENABLED=true to run).
```

Exit 0. No Excel read, no SMTP.

### Batch on, paths unset

```text
Mail batch skipped: set mail.excel-file-path and mail.body-file-path (or MAIL_EXCEL_FILE_PATH / MAIL_BODY_FILE_PATH).
```

Exit 0.

### Dry-run (verified)

```text
Mail batch dry-run: printing To and body; SMTP is skipped (set MAIL_DRY_RUN=false to send).
DRY-RUN To: <address>
<personalized body>
Batch summary: sent=N, failed=0, skipped=M
```

`sent` in dry-run means “would send” (dry-run `EmailService` returns without `JavaMailSender`). Sent-log file is **not** appended. Addresses already listed in the sent-log file are still **skipped**.

### Real send (from code/tests, not live SMTP)

- Success: `Sent message successfully to <to>`; address appended to sent-log; delay `mail.send-delay-ms` before the next attempt (not after the last; not in dry-run; skip if delay is 0).
- Per-recipient SMTP error: warn, continue; other recipients still processed.
- End: `Batch summary: sent=…, failed=…, skipped=…`. If `failed > 0`, throw `IllegalStateException("Batch had N send failure(s)")` (non-zero exit).
- Empty usable list: warn `No recipients to send…`, summary all zeros, exit 0.

---

## Error behavior

| Situation | Behavior |
| --- | --- |
| Batch off | Skip, exit 0 |
| Paths blank | Skip, exit 0 |
| Excel missing / unreadable / not xlsx | `IllegalStateException`, fail before send |
| Body missing | `IllegalStateException` when reading template |
| Real send, missing SMTP user/password/from | `IllegalStateException` before Excel |
| Real send, blank sent-log path | `IllegalStateException` before Excel |
| Real send, attachment path set but unreadable | `IllegalStateException` before Excel |
| Attachment missing at MIME time | `RuntimeException` from `MailBodyAttachment` |
| Header email without name (and col A not an email) | `IllegalStateException` |
| Blank / invalid email rows | Skip row, continue |
| One SMTP failure | Continue; process fails at end if any failed |
| Sent-log write fails after SMTP success | Warn; not counted as send failure |
| Interrupt during delay | `IllegalStateException` |

Stack traces may appear for wrapped I/O and SMTP errors. User-facing messages are mostly `IllegalStateException` / `RuntimeException` strings, not a typed exception hierarchy.

---

## Current workflow (diagram)

```text
mvn spring-boot:run
        ↓
MailSenderApplication (WebApplicationType.NONE)
        ↓
BatchMailRunner
        ├── batch-enabled false → log skip → exit 0
        ├── excel or body path blank → log skip → exit 0
        ├── dry-run false → require SMTP + sent-log + readable attachment
        ↓
ReadFromExcel.readEmailsAndNamesFromExcel
        ↓
MailBody.sendPersonalizedEmails
        ├── UTF-8 template
        ├── load sent-log; skip already sent
        ├── personalize {{placeholders}}
        └── EmailService.sendEmail
                ├── dry-run: log To + body
                └── else: JavaMailSender + optional attachment + sent-log append
```

---

## Tests as baseline evidence

Existing tests (no real SMTP): Excel header/formula/invalid rows, extra columns, missing files, personalize, dry-run does not call `JavaMailSender`, send-loop continues after one failure, sent-log skip/write, attachment omitted vs missing, batch runner credential/attachment gates, Spring context load with batch off.

---

## Security reminder (I1)

Do not commit SMTP passwords. If a Gmail App Password was ever in git history, revoke it in the Google Account. This baseline pass did not rotate credentials or rewrite history.
