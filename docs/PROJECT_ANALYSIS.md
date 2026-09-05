# Project analysis

Originally **M1-001** (discovery snapshot). Updated **M1-027** so developers can find the current CLI. The snapshot below the divider is kept for history; prefer the current section and [ARCHITECTURE.md](ARCHITECTURE.md) for work today.

## Current stack (M1-027)

| Item | Value |
| --- | --- |
| Language | Java 17 (`pom.xml` `java.version`) |
| Framework | Spring Boot 3.2.3 |
| App type | Non-web CLI: `WebApplicationType.NONE` |
| Mail | `spring-boot-starter-mail` behind `EmailSender` / `SmtpEmailSender` |
| Excel | Apache POI `poi-ooxml` 5.2.3, `.xlsx` only |
| Logging | SLF4J; `ApplicationLifecycleLogger` plus campaign/Excel/SMTP events |
| Tests | JUnit 5, Mockito; JaCoCo report for `excel` / `template` / `campaign` after `mvn test` |
| Formatting | Spotless 2.43.0 (Google Java Format) |

Entry: `MailSenderApplication` enables `MailAppProperties` and `MailProperties`. `BatchMailRunner` runs test-send **or** Excel batch, then the JVM exits.

## Current source layout

```text
src/main/java/com/mailSender/
  MailSenderApplication.java, BatchMailRunner.java, MailBody.java,
  MailBodyAttachment.java, SentAddressLog.java
  campaign/   EmailMessage, EmailComposer
  excel/      ExcelReader, ExcelValidator, Contact, ExcelReadResult, ExcelProcessingException
  template/   EmailTemplate, TemplateRenderer, TemplateValidator, TemplateValidationException
  smtp/       EmailSender, SmtpEmailSender, SmtpFailureClassifier, SmtpSendException, EmailSendingException
  config/     MailAppProperties, SmtpConfiguration, SmtpConfigurationException, ApplicationLifecycleLogger
```

Resources: `application.properties` (safe defaults: batch off, dry-run on), `application-development.properties`, `application-production.properties` (no passwords), `application-local.properties.example`.

Tests live next to those packages (`ExcelReaderTest`, `TemplateRendererTest`, `SmtpEmailSenderTest`, `EndToEndLocalWorkflowTest`, `OriginalExcelSmtpRegressionTest`, …). They mock `JavaMailSender` / `EmailSender` and do not send real mail.

## Current pipeline

```text
MailSenderApplication (NONE web)
        ↓
BatchMailRunner
        ├── test-send → first Excel row placeholders (optional) → MailBody.sendTestEmail
        └── batch → ExcelReader → MailBody.sendPersonalizedEmails
                ├── TemplateValidator + EmailComposer
                └── EmailSender.send (SmtpEmailSender dry-run or MIME)
```

## Testing status (M1-027)

Excel, template, validation, mocked SMTP failure modes, local test-send e2e, and original batch regression are covered. There is still no live SMTP integration test (by design).

## Remaining (not Milestone 2)

See [TECHNICAL_DEBT.md](TECHNICAL_DEBT.md) “Still open”. Milestone 1 docs IDs after this one: Excel format file (M1-028), cleanup, dependency review, verify, secret scan, architecture review, `NEXT_MILESTONE.md`.

---

# Discovery snapshot (M1-001)

Inspection of the existing Excel → SMTP command-line application **as of discovery**. It does not describe today’s package names.

## Technology Stack


| Item | Value |
| --- | --- |
| Language | Java 17 (`pom.xml` `java.version`) |
| Framework | Spring Boot 3.2.3 (`spring-boot-starter-parent`) |
| App type | Non-web CLI: `WebApplicationType.NONE` (no `spring-boot-starter-web`) |
| Mail | `spring-boot-starter-mail` → `JavaMailSender` / `MimeMessageHelper` |
| Excel | Apache POI `poi-ooxml` 5.2.3 (`XSSFWorkbook`, `.xlsx` only) |
| Logging | SLF4J via Spring Boot (`LoggerFactory` on runners/services) |
| Tests | `spring-boot-starter-test` (JUnit 5, Mockito) |
| Formatting | Spotless Maven plugin 2.43.0 (Google Java Format) |

Default SMTP in config is Gmail (`smtp.gmail.com:587`, STARTTLS, auth). Credentials are not hardcoded; they come from env / optional local properties.

## Application Entry Point

- Class: `com.mailSender.MailSenderApplication`
- File: `src/main/java/com/mailSender/MailSenderApplication.java`
- `main` builds `SpringApplication`, sets `WebApplicationType.NONE`, then `run`.
- `@EnableConfigurationProperties(MailAppProperties.class)` binds `mail.*`.
- Batch work runs after context start via `BatchMailRunner` (`CommandLineRunner`). If `mail.batch-enabled` is false (default), the runner logs a skip message and the JVM exits without sending.

Run locally: `mvn spring-boot:run` (with env or `application-local.properties`). Tests: `mvn test`.

## Build System

- Maven (`pom.xml`)
- Coordinates: `com.learning:MailSender:0.0.1-SNAPSHOT`
- Plugins: `spring-boot-maven-plugin`, `spotless-maven-plugin`
- No Maven Wrapper files present in the tree inspected (`mvnw` / `.mvn` not listed)

## Source directories

```text
src/main/java/com/mailSender/
  MailSenderApplication.java   # Spring Boot entry
  BatchMailRunner.java         # CLI batch orchestration
  MailAppProperties.java       # mail.* binding
  ReadFromExcel.java           # Excel → EmailRecipient list (static)
  EmailRecipient.java          # email, name, placeholder map
  MailBody.java                # UTF-8 template, personalize, send loop
  EmailService.java            # dry-run or SMTP send
  MailBodyAttachment.java      # optional MimeMessage attachment
  SentAddressLog.java          # persist/skip already-sent addresses
```

Single package `com.mailSender`. No `config` / `excel` / `smtp` package split yet.

## Resources / configuration

| File | Role |
| --- | --- |
| `src/main/resources/application.properties` | App name, optional import of local file, `spring.mail.*`, `mail.*` with `${ENV:default}` |
| `src/main/resources/application-local.properties.example` | Placeholder SMTP/paths; copy to gitignored `application-local.properties` |
| `.gitignore` | Ignores `application-local.properties`, `.env`, `sent-addresses.txt`, `target/`, IDE files |

`application.properties` imports `optional:classpath:application-local.properties`. Safe defaults: `mail.batch-enabled=false`, `mail.dry-run=true`.

## Test directories

```text
src/test/java/com/mailSender/
  MailSenderApplicationTests.java
  BatchMailRunnerTest.java
  ReadFromExcelTest.java
  EmailRecipientTest.java
  MailBodyPersonalizeTest.java
  MailBodyReadFileTest.java
  MailBodySendLoopTest.java
  MailBodyAttachmentTest.java
  EmailServiceDryRunTest.java
  SentAddressLogTest.java
```

Tests mock `JavaMailSender` / collaborators; they do not send real SMTP.

## Dependency files

- `pom.xml` — sole build and dependency manifest.
- Runtime/test deps: `spring-boot-starter`, `spring-boot-starter-mail`, `spring-boot-starter-test`, `poi-ooxml`.

## Documentation

| Path | Role |
| --- | --- |
| `README.md` | User/developer usage, Excel format, SMTP, dry-run, env table |
| `AGENTS.md` | Milestone 1 agent instructions |
| `DEVELOPMENT_TASKS.md` | Task list M1-001–M1-035 |
| `AGENT_WORKFLOW.md` | Per-task loop and playbooks |
| `.cursor/rules/` | Cursor rules (mail safety, config, batch, workflow) |
| `docs/` | Created by Milestone 1 discovery (this file is M1-001) |

## Generated / build files

- Maven output: `target/` (gitignored)
- Runtime sent log: `sent-addresses.txt` (gitignored; path configurable)
- IDE/build caches: `.idea`, `.vscode`, `build/`, etc. (gitignored)

## Current Architecture

```text
MailSenderApplication (NONE web)
        ↓
BatchMailRunner (CommandLineRunner)
        ↓
ReadFromExcel.readEmailsAndNamesFromExcel  →  List<EmailRecipient>
        ↓
MailBody.sendPersonalizedEmails
        ├── read UTF-8 template
        ├── SentAddressLog.load / skip already sent
        ├── MailBody.personalize ({{placeholders}})
        └── EmailService.sendEmail
                ├── dry-run: log To + body
                └── else: JavaMailSender + MailBodyAttachment
```

Excel parsing, template rendering, SMTP, and campaign loop live in a few classes in one package. `MailBody` both renders templates and drives the send loop. `ReadFromExcel` is a static utility, not a Spring bean.

## Excel Processing Location

- Primary: `ReadFromExcel.java` (`XSSFWorkbook`, first sheet, header detection, `DataFormatter` + formula evaluator).
- Domain row: `EmailRecipient` (email, name, extra columns as placeholders).
- Tests: `ReadFromExcelTest.java`, `EmailRecipientTest.java`.

## SMTP Processing Location

- Config: `application.properties` (`spring.mail.*`) plus `MailAppProperties` (`mail.from`, subject, dry-run, delay, etc.).
- Send: `EmailService.sendEmail` (`JavaMailSender`).
- Attachments: `MailBodyAttachment`.
- Pre-send checks (username/password/from, sent-log path, attachment readable): `BatchMailRunner`.
- Tests: `EmailServiceDryRunTest.java`, `MailBodySendLoopTest.java`, `BatchMailRunnerTest.java` (mocked sender).

## Configuration Location

- `src/main/resources/application.properties`
- `MailAppProperties` (`@ConfigurationProperties(prefix = "mail")`)
- Env vars documented in README (`MAIL_HOST`, `MAIL_PASSWORD`, `MAIL_BATCH_ENABLED`, …)
- Local overrides: gitignored `application-local.properties` (example file in source)

## Testing Status

Unit and Spring context tests exist for Excel read, personalization, send loop (per-recipient failure), dry-run, attachments, sent log, and batch runner skip/fail-loud paths. There is no dedicated integration test that runs a full Excel → SMTP path against a real server (by design). Coverage of later refactored layers (packages, `EmailSender` interface, template validation reports) is not yet a Milestone 1 architecture concern.

## Known Problems

- All types sit in `com.mailSender`; Milestone 1 architecture packages are not introduced yet.
- `MailBody` mixes template I/O, personalization, delay, sent-log, and send orchestration.
- `ReadFromExcel` is static; harder to mock without wrapping.
- No dedicated `EmailSender` interface; callers depend on `EmailService`.
- SMTP failures are wrapped as generic `RuntimeException`; no typed SMTP/config exceptions.
- Excel: `.xlsx` only (`XSSFWorkbook`); empty/malformed files throw or skip rows rather than returning a structured validation report (valid/invalid/duplicate counts).
- Duplicate emails in one file are not reported as a validation category (sent-log handles re-runs across process starts).
- Unknown `{{placeholders}}` become empty strings; no template validation errors.
- `mail.subject` default is a personal application subject string in `application.properties`.
- README notes that a Gmail App Password may still exist in **git history**; rotate in Google Account (I1). No live password in current source files inspected.

## Refactoring Recommendations

These are for later Milestone 1 tasks, not this discovery pass:

1. Document baseline workflow and technical debt (M1-002, M1-003), then proposed packages (M1-004).
2. Extract Excel read/validate/`Contact` (or keep `EmailRecipient`) without sending mail.
3. Extract template model/renderer and validation.
4. Introduce SMTP config model + `EmailSender` / `SmtpEmailSender`; keep `JavaMailSender` behind the abstraction.
5. Split generate-message vs send; add a single-recipient test-send path that does not iterate the full Excel list.
6. Keep batch off / dry-run defaults; never verify with live SMTP in agent work.
