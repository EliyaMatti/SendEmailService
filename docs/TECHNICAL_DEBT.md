# Technical debt (M1-003)

Findings from inspecting the current Excel → SMTP CLI. **This task does not change code.** Later Milestone 1 IDs should pick items up; do not treat this list as a license to rewrite everything at once.

---

## God classes

- **`MailBody`** combines UTF-8 template I/O, `{{placeholder}}` rendering, sent-log skip logic, inter-send delay, per-recipient try/catch, summary logging, and process-level failure (`failed > 0` → throw). Template work and campaign orchestration should split (M1-007, M1-014).
- **`ReadFromExcel`** is a single static entry point plus a nested `ColumnMap` that owns header detection, aliases, extra columns, and row extraction. Validation (empty file, duplicates, structured counts) is not a separate type (M1-005, M1-006).
- **`BatchMailRunner`** is the only “application” orchestrator: feature flags, SMTP presence checks, attachment preflight, Excel read, then `MailBody`. Acceptable size today; it will grow if test-send and validation reports are stuffed in without new types (M1-015).

There is no 1,000-line class. Coupling is more of a problem than raw size.

## Large methods

- `ReadFromExcel.ColumnMap.fromFirstRow` — header vs headerless branching in one method.
- `MailBody.sendPersonalizedEmails` — full campaign loop (~50 lines) with several side effects.
- `EmailService.sendEmail` — dry-run vs MIME send in one method.

None are unreadable; they mix responsibilities.

## Duplicate code

- Blank-string checks: `BatchMailRunner.isBlank` vs `configured == null || isBlank()` in `SentAddressLog` vs similar guards in `MailBodyAttachment` / `BatchMailRunner` for paths.
- “File missing or unreadable” is implemented separately for Excel (`Files.isRegularFile`), body (`Files.readString` IOException), attachment (`File.isFile` / `canRead` in the runner **and** `exists` / `canRead` in `MailBodyAttachment`).
- Email “validity” is only `contains("@")` at Excel read time; SMTP `setTo` can still fail later. No shared validator.

## Hardcoded values

- Gmail host/port and STARTTLS/auth flags in `application.properties`.
- Default subject string is a personal job-application line, not a generic product default.
- Default sent-log filename `sent-addresses.txt`.
- Java field defaults in `MailAppProperties` duplicate (and can disagree with) `application.properties` placeholders.

## Hardcoded credentials

- **Source (committed):** no live password in `application.properties` (empty `${MAIL_PASSWORD:}`). Example file uses placeholders only.
- **Gitignored `application-local.properties`:** present on this workspace and contains a **real Gmail username and App Password**. That file must stay uncommitted. **Rotate that App Password in Google Account** (I1): it was loaded into an agent session via repository search. Also rotate any password that still exists in **git history**.
- Tests use fake strings (`secret`, `user@example.com`), not live credentials.

## Static state

- No mutable global caches or static recipient lists.
- `ReadFromExcel` is a static utility (harder to mock without wrapping).
- `MailBody.readFileContent` / `personalize` are static (testable, but not an injectable template service).
- `SentAddressLog.normalize` is static (fine).

## Tight coupling

- Single package `com.mailSender`; no excel / template / smtp layers.
- Callers depend on concrete `EmailService`, not an `EmailSender` interface (M1-011).
- `EmailService` depends on `JavaMailSender` + `MailBodyAttachment` + `MailAppProperties`.
- `MailBody` depends on `EmailService` + `SentAddressLog` + `MailAppProperties` (generation mixed with send).
- SMTP host/port live on `spring.mail.*`; from/subject/dry-run live on `mail.*` — two config models (M1-009, M1-016).
- No `EmailMessage` type: send is `sendEmail(String to, String body)` (M1-013).

## Poor exception handling

- Broad `catch (Exception)` in `EmailService` and `ReadFromExcel`.
- SMTP, I/O, and config errors are `RuntimeException` / `IllegalStateException` with little typing (`EmailSendingException`, `SmtpConfigurationException`, etc. — M1-019).
- Authentication vs timeout vs invalid recipient are not distinguished (M1-012).
- Batch failure after partial sends throws a count only; no per-address error report for the operator beyond logs.
- Attachment failure modes differ: preflight `IllegalStateException` vs in-send `RuntimeException("Cannot read file: …")`.

## Poor logging

- SLF4J is used on runners/services (no `System.out` / `printStackTrace` on those paths).
- Dry-run logs **full body** and To-address (PII). Success/failure logs the recipient address.
- No structured event for “Excel loaded” with valid/invalid/duplicate counts (M1-006, M1-018).
- No explicit SMTP connection success log before the loop (connection happens per send).
- No application-shutdown hook/log beyond JVM exit.
- Password is not logged (good). `BatchMailRunner` holds password in memory only for a blank-check.

## Unclear naming

- Artifact/app name `MailSender` vs repo `SendEmailService` vs future product “ExcelMail Pro”.
- `MailBody` is not “the body”; it is the campaign + template helper.
- `ReadFromExcel` is a verb phrase, not a reader type.
- `EmailRecipient` vs planned `Contact`.
- `sent` in dry-run means “previewed,” not SMTP accepted.
- `mail.from` vs `spring.mail.username` duplication.

## Unused dependencies

- `pom.xml`: `spring-boot-starter`, `spring-boot-starter-mail`, `poi-ooxml`, `spring-boot-starter-test` — all used.
- No `spring-boot-starter-web` (good).
- Spotless is a plugin only (good).
- No Maven Wrapper in the repo (environment friction, not unused deps).

## Unused methods

- All public production methods are referenced from runners, services, or tests. No obvious dead private methods found in this pass.
- `EmailRecipient` two-arg constructor is used.

## Magic numbers / strings

- Header aliases (`email`, `e_mail`, `mail`, `name`, …) are string literals in `ReadFromExcel`.
- Placeholder regex `\{\{(\w+)\}\}` — unknown keys silently empty (M1-008).
- Excel “valid email” = contains `@` (no RFC check).
- Column A = 0, column B = 1 in headerless mode.
- First sheet index `0`.
- Delay default `1000` appears in properties, Java defaults, and example file.

## Logging / config drift

- README and `MailAppProperties` say batch default **false**, dry-run **true**.
- Git `HEAD` `application.properties` matches that.
- **Uncommitted** `application.properties` currently defaults batch **true** and dry-run **false**. That is operational debt: easy to hit live SMTP with local properties.

## Testing gaps (debt, not a fail)

- No JaCoCo/coverage gate yet (M1-023 target ~80% on refactored core).
- No typed SMTP failure tests against a mock transport (auth/timeout/rejection) beyond generic `RuntimeException` (M1-024).
- No dedicated test-send API (M1-015).
- `@SpringBootTest` uses `@MockBean` (works on Boot 3.2.3; watch deprecation on newer Boot).
- End-to-end against real SMTP is intentionally absent.

## Other

- `.xlsx` only; `.xls` / CSV unsupported.
- Duplicate emails in one workbook are not a validation category (in-run skip after first success only).
- Empty workbook / header-only yields empty list and exit 0, not a validation report.
- No `development` / `production` Spring profiles (M1-017).
- Subject is not placeholder-aware.
- No reply-to, CC, or `EmailMessage.attachments` collection (single optional path).
- Personal default subject and local App Password make the tree feel like a private job-mail tool rather than a reusable core.
