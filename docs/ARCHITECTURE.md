# Architecture (M1-004)

Proposed package layout for the existing Excel → SMTP CLI (`com.mailSender`). This document does **not** create packages or classes. Later Milestone 1 tasks implement the split incrementally.

The sketch in `DEVELOPMENT_TASKS.md` (ExcelMailPro `config` / `excel` / `template` / `smtp` / `campaign` / `validation` / `logging` / `application`) is a **responsibility map**, not a mandate to invent unused types. Names below keep the current Spring Boot artifact (`MailSender`) and map each box to what already exists.

---

## Current layout (after M1-016)

```text
com.mailSender
├── MailSenderApplication
├── BatchMailRunner            # CLI: test-send or Excel batch
├── campaign
│   ├── EmailMessage
│   └── EmailComposer
├── excel
│   ├── ExcelReader
│   ├── ExcelValidator
│   ├── ExcelReadResult
│   ├── ExcelProcessingException
│   └── Contact
├── template
│   ├── EmailTemplate
│   ├── TemplateRenderer
│   ├── TemplateValidator
│   └── TemplateValidationException
├── smtp
│   ├── EmailSender
│   ├── SmtpEmailSender
│   ├── SmtpFailureClassifier
│   ├── SmtpSendException
│   └── EmailSendingException
├── config
│   ├── MailAppProperties      # mail.* files, sending, envelope
│   ├── SmtpConfiguration      # spring.mail.* + mail.from / from-name
│   ├── SmtpConfigurationException
│   └── ApplicationLifecycleLogger
├── MailBody
├── MailBodyAttachment
└── SentAddressLog
```

Pipeline:

```text
Excel file
    → ExcelReader / ExcelValidator
    → Contact
    → EmailComposer (TemplateRenderer)
    → EmailMessage
    → EmailSender
    → SmtpEmailSender | dry-run
```

Test send: same pipeline for **one** address (`mail.test-send-to`); the Excel list is not mailed.

---

## Target layout (adapt, do not copy blindly)

Keep the root `com.mailSender`. Add **subpackages** only when a later ID extracts a type. Do not introduce empty packages or SaaS-only folders (API, auth, persistence).

```text
com.mailSender
│
├── MailSenderApplication          # application (entry)
│
├── config
│   ├── MailAppProperties          # from root (M1-016)
│   └── SmtpConfiguration          # host/port/user/from/tls (M1-009); password not logged
│
├── excel
│   ├── ExcelReader                # from ReadFromExcel (M1-005)
│   ├── ExcelValidator             # row/file reports (M1-006)
│   └── Contact                    # from EmailRecipient (email, name, extras)
│
├── template
│   ├── EmailTemplate              # UTF-8 body + subject (M1-007)
│   ├── TemplateRenderer           # {{placeholders}} (M1-007)
│   └── TemplateValidator          # empty body/subject, unknown keys (M1-008)
│
├── smtp
│   ├── EmailSender                # interface (M1-011)
│   ├── SmtpEmailSender            # from EmailService real path
│   ├── AttachmentSupport          # from MailBodyAttachment
│   └── (dry-run)                  # EmailSender impl or flag inside sender — pick at M1-011
│
├── campaign
│   ├── EmailCampaign              # from MailBody.sendPersonalizedEmails (M1-014)
│   ├── EmailMessage               # to/from/subject/body/attachments (M1-013)
│   └── SentAddressLog             # skip/append; dry-run must not write
│
├── validation                     # optional: shared email-format / blank checks
│   └── (small helpers only if duplicated across excel + template)
│
├── exception                      # typed failures (M1-019), not a logging layer
│
└── application
    └── BatchMailRunner            # CLI orchestration only (M1-015 test-send stays here or campaign)
```

**Logging:** do not add a `logging` package of wrappers. Use SLF4J on the types that own events (M1-018). A package named `logging` would only be justified for a sent-log **file** type; that stays next to campaign as `SentAddressLog`.

**Validation:** Excel and template validation belong with those modules (M1-006, M1-008). A top-level `validation` package is for shared predicates (e.g. “contains `@`”), not a second campaign orchestrator.

---

## Module responsibilities

| Module | Owns | Must not own |
| --- | --- | --- |
| **excel** | Read `.xlsx`, headers/aliases, rows → `Contact`, skip/report bad rows | SMTP, templates, sent-log, CLI flags |
| **template** | Load UTF-8 body, render `{{word}}`, template checks | Excel I/O, `JavaMailSender` |
| **smtp** | `EmailSender`, MIME/SMTP, attachment on the message, dry-run vs send | Excel parse, placeholder regex |
| **campaign** | Loop: skip sent, render → `EmailMessage` → send, delay, summary counts | POI, Boot `spring.mail` wiring details |
| **config** | Bind env/`mail.*`/`spring.mail.*` into typed objects | Business rules |
| **application** | Start/stop, batch vs skip, test-send of **one** address (M1-015), preflight | Parsing Excel cells |

Target pipeline (Milestone 1 end state, same as §24 in `DEVELOPMENT_TASKS.md`):

```text
Excel file
    → ExcelReader / ExcelValidator
    → Contact
    → TemplateRenderer
    → EmailMessage
    → EmailSender
    → SmtpEmailSender | dry-run
```

---

## Mapping: existing types → target

| Today | Target | First implementing ID |
| --- | --- | --- |
| `ReadFromExcel` | `excel.ExcelReader` | M1-005 |
| Nested `ColumnMap` | stay inside reader or `excel` helper | M1-005 |
| `EmailRecipient` | `excel.Contact` (keep extra-column map) | M1-005 |
| Row skip / `@` check | `excel.ExcelValidator` + result object | M1-006 |
| `MailBody.readFileContent` / `personalize` | `template.EmailTemplate` / `TemplateRenderer` | M1-007 |
| Unknown `{{}}` / empty body | `template.TemplateValidator` | M1-008 |
| `spring.mail.*` + parts of `MailAppProperties` | `config.SmtpConfiguration` | M1-009 |
| `EmailService` | `smtp.EmailSender` + `SmtpEmailSender` | M1-011 |
| `MailBodyAttachment` | smtp helper used when building MIME | M1-011 / M1-013 |
| `sendEmail(String, String)` | `EmailMessage` then `EmailSender.send` | M1-013 |
| `MailBody.sendPersonalizedEmails` | `campaign.EmailCampaign` | M1-014 |
| `SentAddressLog` | stay in campaign (file I/O, not SLF4J) | already exists; move with campaign |
| `BatchMailRunner` | `application` orchestrator | stays; thinner after extracts |
| `MailAppProperties` | `config` (paths, delay, html, dry-run, batch) | M1-016 |
| `IllegalStateException` / `RuntimeException` | typed `ExcelProcessingException`, `TemplateValidationException`, `SmtpConfigurationException`, `EmailSendingException` (`SmtpSendException` subclass) | M1-019 |

Rename only when extracting. Call sites and tests update in the same ID as the move.

---

## What we will not add in Milestone 1

- REST controllers, security, PostgreSQL, Redis, Kafka
- A `logging` service that re-wraps SLF4J
- Duplicate “Contact” and “EmailRecipient” types long-term (one domain model)
- A second config system besides Spring (`MailAppProperties` + `SmtpConfiguration` is enough)
- Packages created “for completeness” with no class

---

## Constraints that stay true after the split

- Non-web: `WebApplicationType.NONE`; no `starter-web`
- Default: batch off, dry-run on; real SMTP only when batch on **and** dry-run false
- Dry-run must not call `JavaMailSender` and must not append the sent-log
- Per-recipient send failures continue the loop; process fails at end if any failed
- No secrets in git; password never logged
- Profiles: default `development`; `production` has no credentials in the committed file

---

## Implementation order (packages appear when code moves)

1. M1-005 / M1-006 — `excel` (+ `Contact`)
2. M1-007 / M1-008 — `template`
3. M1-009 / M1-011 / M1-012 — `config.SmtpConfiguration`, `smtp`
4. M1-013 / M1-014 — `EmailMessage`, `campaign`
5. M1-016 / M1-017 — remaining config / profiles
6. M1-018 / M1-019 — log events on existing types; `exception` types as needed

Until remaining IDs run, see `docs/BASELINE.md` for pre-refactor behavior. Excel types now live in `com.mailSender.excel`.
