# SendEmailService — work list by severity

Work **High → Medium → Low**. Check items off as they land. Tests for a change should ship with that change when possible.

---

## High — correctness and accidental sends

- [ ] **H1. Per-recipient send failures**
  - Catch errors in the batch loop; log the address; continue with the rest.
  - Print a summary: sent / failed / skipped.
  - Files: `EmailService.java`, `BatchMailRunner.java`, `MailBody.java`

- [ ] **H2. Safe default / dry-run**
  - Do not send on every `mvn spring-boot:run` by accident.
  - Default `mail.batch-enabled` to `false`, **or** add `MAIL_DRY_RUN` that prints To + body and skips SMTP.
  - Files: `application.properties`, `BatchMailRunner.java`, README

- [ ] **H3. Excel recipient parsing**
  - Skip header row; trim cells; skip blank/invalid emails.
  - Read formula/numeric cells (`DataFormatter`), not only `STRING`.
  - Files: `ReadFromExcel.java`

- [ ] **H4. Fail loud on missing input files**
  - Excel or body-file I/O errors must fail the job (non-zero exit / exception), not return empty/null and look successful.
  - Files: `ReadFromExcel.java`, `MailBody.java`, `BatchMailRunner.java`

- [ ] **H5. Idempotency (sent log)**
  - Record successful addresses; skip them on re-run so the list is not mailed twice.
  - Files: new helper + `BatchMailRunner` / `MailBody`

---

## Medium — product correctness

- [ ] **M1. UTF-8 body template**
  - Read the body file as UTF-8 (not platform default `FileReader`).
  - File: `MailBody.java`

- [ ] **M2. HTML vs plain text**
  - Config flag (e.g. `mail.html`) so templates can be `text/html`.
  - Files: `EmailService.java`, `MailAppProperties.java`, `application.properties`

- [ ] **M3. Extra Excel columns → placeholders**
  - Map column headers to `{{key}}` (not only `name` / `email`).
  - Files: `ReadFromExcel.java`, `EmailRecipient.java`

- [ ] **M4. SMTP config check before the loop**
  - Require username, password, and from-address when a real send is enabled.
  - Files: `BatchMailRunner.java` / `EmailService.java`

- [ ] **M5. Rate limit**
  - Delay between messages so Gmail is less likely to throttle the batch.
  - Files: `MailAppProperties.java`, `MailBody.java` or `EmailService.java`

- [ ] **M6. Tests (currently only `contextLoads`)**
  - `MailBody.personalize` (missing keys, special chars).
  - Excel: header skip, formula cells, invalid email, extra columns.
  - Batch: one send failure does not stop later recipients (after H1).
  - Attachment: missing file vs omitted path.
  - Files: `src/test/java/...`

---

## Low — cleanup

- [ ] **L1. `pom.xml`**
  - Remove duplicate `spring-boot-maven-plugin`.
  - Spotless as plugin only (not a compile dependency); align versions.
  - Drop unused POI extras if they are only transitive.

- [ ] **L2. No unused web server**
  - Remove `spring-boot-starter-web` or set `WebApplicationType.NONE` unless an HTTP API is added.

- [ ] **L3. Logging**
  - Replace `System.out` / `printStackTrace` with SLF4J.

- [ ] **L4. Immutable placeholders**
  - Return an unmodifiable copy from `EmailRecipient.getPlaceholders()`.

---

## Info (not code, still required)

- [ ] **I1. Rotate any Gmail App Password** that was ever committed; treat git history as leaked.

---

## Suggested order

1. H2 (stop accidental sends)  
2. H4 + H3 (trust the input)  
3. H1 (do not abort the whole list)  
4. M1, M6 (charset + tests around H1–H4)  
5. H5, then M2–M5  
6. L1–L4, I1  
