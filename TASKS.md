# SendEmailService — work list by severity

Work **High → Medium → Low**. Check items off as they land. Tests for a change should ship with that change when possible.

Agents: start with [AGENTS.md](AGENTS.md), then follow [AGENT_WORKFLOW.md](AGENT_WORKFLOW.md) (one ID per pass, suggested order, definition of done). Project rules: `.cursor/rules/`.

---

## High — correctness and accidental sends

- [x] **H1. Per-recipient send failures**
  - Catch errors in the batch loop; log the address; continue with the rest.
  - Print a summary: sent / failed / skipped.
  - Files: `EmailService.java`, `BatchMailRunner.java`, `MailBody.java`

- [x] **H2. Safe default / dry-run**
  - Do not send on every `mvn spring-boot:run` by accident.
  - Default `mail.batch-enabled` to `false`, **or** add `MAIL_DRY_RUN` that prints To + body and skips SMTP.
  - Files: `application.properties`, `BatchMailRunner.java`, README

- [x] **H3. Excel recipient parsing**
  - Skip header row; trim cells; skip blank/invalid emails.
  - Read formula/numeric cells (`DataFormatter`), not only `STRING`.
  - Files: `ReadFromExcel.java`

- [x] **H4. Fail loud on missing input files**
  - Excel or body-file I/O errors must fail the job (non-zero exit / exception), not return empty/null and look successful.
  - Files: `ReadFromExcel.java`, `MailBody.java`, `BatchMailRunner.java`

- [x] **H5. Idempotency (sent log)**
  - Record successful addresses; skip them on re-run so the list is not mailed twice.
  - Files: new helper + `BatchMailRunner` / `MailBody`

- [x] **H6. Sent log on by default for real sends**
  - Empty `mail.sent-log-path` makes H5 a no-op: a second real run mails the whole list again.
  - Default a gitignored local path, **or** refuse a real send (`dry-run=false`) when the path is blank.
  - Document `MAIL_SENT_LOG_PATH` on the README “send for real” example.
  - Keep `mail.*` in sync: `application.properties`, `MailAppProperties`, example file, README.
  - Files: `application.properties`, `BatchMailRunner.java` / `SentAddressLog.java`, README

- [x] **H7. Skip duplicates in the same run; log I/O ≠ SMTP failure**
  - After a successful SMTP send, add the normalized address to the in-memory skip set (same file, mixed case).
  - If `SentAddressLog.record` throws after SMTP succeeded, do not count that recipient as a send failure; remaining recipients must still be attempted.
  - Files: `MailBody.java`, tests in `MailBodySendLoopTest.java`

- [x] **H8. No DevTools on the send-on-startup CLI**
  - `spring-boot-devtools` live-reload re-runs `BatchMailRunner` and can duplicate a real batch.
  - Remove the dependency, or exclude it from `spring-boot-maven-plugin` / `spring-boot:run`.
  - Files: `pom.xml`

---

## Medium — product correctness

- [x] **M1. UTF-8 body template**
  - Read the body file as UTF-8 (not platform default `FileReader`).
  - File: `MailBody.java`

- [x] **M2. HTML vs plain text**
  - Config flag (e.g. `mail.html`) so templates can be `text/html`.
  - Files: `EmailService.java`, `MailAppProperties.java`, `application.properties`

- [x] **M3. Extra Excel columns → placeholders**
  - Map column headers to `{{key}}` (not only `name` / `email`).
  - Files: `ReadFromExcel.java`, `EmailRecipient.java`

- [x] **M4. SMTP config check before the loop**
  - Require username, password, and from-address when a real send is enabled.
  - Files: `BatchMailRunner.java` / `EmailService.java`

- [x] **M5. Rate limit**
  - Delay between messages so Gmail is less likely to throttle the batch.
  - Files: `MailAppProperties.java`, `MailBody.java` or `EmailService.java`

- [x] **M6. Tests (currently only `contextLoads`)**
  - `MailBody.personalize` (missing keys, special chars).
  - Excel: header skip, formula cells, invalid email, extra columns.
  - Batch: one send failure does not stop later recipients (after H1).
  - Attachment: missing file vs omitted path.
  - Files: `src/test/java/...`

- [x] **M7. Non-zero exit when the batch had send failures**
  - Today `sent/failed/skipped` is logged and the process still exits 0 if `failed > 0`.
  - After the loop, fail the job (throw or `SpringApplication.exit` non-zero) when any recipient failed SMTP.
  - Files: `MailBody.java`, `BatchMailRunner.java`, `MailBodySendLoopTest.java`

- [x] **M8. Header detection: require both email and name**
  - Treat row 0 as headers only when **both** an email column and a name column are present (not `emailIndex >= 0 || nameIndex >= 0`).
  - A headerless sheet whose first name is `Name` / `Email` / `Full Name` must use A=email, B=name, not throw.
  - Header row with `email` but no `name` still fails loud (M3).
  - Files: `ReadFromExcel.java`, `ReadFromExcelTest.java`

- [x] **M9. Tests for remaining batch/Excel/runner gaps**
  - In-file duplicate / mixed-case address skipped after first success (with H7).
  - `record()` failure after SMTP success is not counted as a send failure (with H7).
  - Headerless A/B layout; header row missing `name`.
  - `BatchMailRunner`: missing attachment fails before send when dry-run is off.
  - Files: `src/test/java/...`

---

## Low — cleanup

- [x] **L1. `pom.xml`**
  - Remove duplicate `spring-boot-maven-plugin`.
  - Spotless as plugin only (not a compile dependency); align versions.
  - Drop unused POI extras if they are only transitive.

- [x] **L2. No unused web server**
  - Remove `spring-boot-starter-web` or set `WebApplicationType.NONE` unless an HTTP API is added.

- [x] **L3. Logging**
  - Replace `System.out` / `printStackTrace` with SLF4J.

- [x] **L4. Immutable placeholders**
  - Return an unmodifiable copy from `EmailRecipient.getPlaceholders()`.

- [x] **L5. Document dry-run vs sent-log skips**
  - Dry-run still skips addresses already in the sent log; say so in the README (preview of a re-run hides those rows).
  - File: README

- [x] **L6. Excel open errors besides `IOException`**
  - Wrap POI failures (corrupt / not xlsx) as `Cannot read Excel file` so H4 messages stay consistent.
  - File: `ReadFromExcel.java`

- [x] **L7. Warn on empty recipient list**
  - Header-only or all-rows-skipped currently logs `sent=0, failed=0, skipped=0` and exits 0.
  - Log a clear warning (optional: fail the job). Do not treat a missing file as this case (H4).
  - Files: `MailBody.java` / `BatchMailRunner.java`

---

## Info (not code, still required)

- [x] **I1. Rotate any Gmail App Password** that was ever committed; treat git history as leaked.

---

## Suggested order

Closed (do not reopen unless a regression): H2, H4+H3, H1, M1+M6, H5, M2–M5, L1–L4, H6–H8, M8, M7, M9, L5–L7.

Remaining (user action, not code):

1. I1 (rotate any Gmail App Password that was ever committed)
