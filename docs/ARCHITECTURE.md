# Architecture

Excel → SMTP **command-line** app (`com.mailSender`). Non-web Spring Boot 3.2. This document describes the layout **as of M1-034** (layered pipeline verified). It is not a SaaS design.

The sketch in `DEVELOPMENT_TASKS.md` (`config` / `excel` / `template` / `smtp` / `campaign` / …) is a **responsibility map**. Empty packages and Milestone 2 features (APIs, auth, PostgreSQL) stay out of scope.

---

## Current layout

```text
com.mailSender
├── MailSenderApplication          # WebApplicationType.NONE; MailAppProperties + MailProperties
├── BatchMailRunner                # CLI: test-send or Excel batch; SMTP preflight
├── MailBody                       # campaign loop + test-send (uses EmailComposer)
├── MailBodyAttachment             # optional MIME attachment
├── SentAddressLog                 # skip/append already-sent addresses
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
└── config
    ├── MailAppProperties          # mail.* files, sending, envelope
    ├── SmtpConfiguration          # spring.mail.* + mail.from / from-name
    ├── SmtpConfigurationException
    └── ApplicationLifecycleLogger
```

Pipeline (M1-034):

```text
Excel Layer          excel.ExcelReader / ExcelValidator
     ↓
Domain Model         excel.Contact
     ↓
Template Layer       template.EmailTemplate / TemplateRenderer / TemplateValidator
     ↓
Email Model          campaign.EmailMessage (built by EmailComposer)
     ↓
EmailSender          smtp.EmailSender
     ↓
SMTP                 smtp.SmtpEmailSender | dry-run (no JavaMailSender)
```

CLI wiring (`BatchMailRunner`, `MailBody`) sits **above** these packages and orchestrates the flow. It is not a reverse dependency from Excel or template back to SMTP.

### Package dependencies (no layer cycles)

Allowed one-way edges:

```text
template  → excel.Contact
campaign  → excel.Contact, template.TemplateRenderer, config.MailAppProperties
smtp      → campaign.EmailMessage, config.MailAppProperties
config    → (Spring MailProperties / mail.* only; no excel/template/campaign/smtp)
excel     → (POI / JDK only)
```

`ArchitectureLayeringTest` fails if excel/template/campaign/smtp/config import a forbidden upper or sibling package.

**Not a layer cycle:** `SmtpEmailSender` uses root `MailBodyAttachment`, which throws `smtp.EmailSendingException`. That is application-root ↔ SMTP helper coupling left from the original `MailBody` split. Excel, template, and `EmailMessage` stay free of SMTP types.

Runtime path (same layers): Excel file → `ExcelReader` → `Contact` → `EmailComposer` / `TemplateRenderer` → `EmailMessage` → `EmailSender` → `SmtpEmailSender` (or dry-run).

**Test send:** same pipeline for **one** address (`mail.test-send-to`). The Excel list is not mailed. Placeholders come from the first valid Excel row when a path is set.

**Batch:** one composed message per remaining contact (shared `mail.subject`). Dry-run still calls `EmailSender.send` but `SmtpEmailSender` only logs To + body.

---

## Module responsibilities

| Module | Owns | Must not own |
| --- | --- | --- |
| **excel** | Read `.xlsx`, headers/aliases, rows → `Contact`, skip/report bad rows | SMTP, templates, sent-log, CLI flags |
| **template** | Load UTF-8 body, render `{{word}}`, template checks | Excel I/O, `JavaMailSender` |
| **smtp** | `EmailSender`, MIME/SMTP, attachment on the message, dry-run vs send | Excel parse, placeholder regex |
| **campaign** | `EmailMessage` / `EmailComposer`; loop still in `MailBody` | POI, Boot `spring.mail` wiring details |
| **config** | Bind env/`mail.*`/`spring.mail.*` into typed objects | Business rules |
| **application** | Start/stop, batch vs skip, test-send, preflight | Parsing Excel cells |

`MailBody` was not renamed to `EmailCampaign` (M1-014 extracted compose/send, not the class name). `SentAddressLog` and `MailBodyAttachment` remain in the root package.

---

## Mapping: pre-refactor types → today

| Original | Now |
| --- | --- |
| `ReadFromExcel` | `excel.ExcelReader` |
| `EmailRecipient` | `excel.Contact` |
| Row skip / `@` check | `excel.ExcelValidator` + `ExcelReadResult` |
| `MailBody.readFileContent` / `personalize` | `template.EmailTemplate` / `TemplateRenderer` |
| Unknown `{{}}` / empty body | `template.TemplateValidator` |
| `spring.mail.*` + envelope | `config.SmtpConfiguration` + `MailAppProperties` |
| `EmailService` | `smtp.EmailSender` + `SmtpEmailSender` |
| `sendEmail(String, String)` | `EmailMessage` then `EmailSender.send` |
| `IllegalStateException` for operator errors | typed exceptions (M1-019 / M1-020) |

---

## What we will not add in Milestone 1

- REST controllers, security, PostgreSQL, Redis, Kafka
- A `logging` service that re-wraps SLF4J
- A second config system besides Spring
- Packages created “for completeness” with no class

---

## Constraints

- Non-web: `WebApplicationType.NONE`; no `starter-web`
- Default: batch off, dry-run on; real SMTP when dry-run is false **and** (batch **or** test-send)
- Dry-run must not call `JavaMailSender` and must not append the sent-log
- Per-recipient send failures continue the loop; process fails at end if any failed
- No secrets in git; password never logged (`SmtpConfiguration.toString()` uses `password=***`)
- Profiles: default `development`; `production` has no credentials in the committed file

Operator docs: [README.md](../README.md). Discovery-era baseline (do not treat as current runtime): [BASELINE.md](BASELINE.md).
