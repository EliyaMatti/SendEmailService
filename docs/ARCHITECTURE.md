# Architecture

ExcelMail Pro is a **dual-mode** Spring Boot 3.2 application (`com.mailSender`):

- **CLI (default):** `WebApplicationType.NONE`, no PostgreSQL, `BatchMailRunner` (`@Profile("!api")`).
- **API (`api` profile):** servlet, Flyway/JPA, JWT, `/api/v1/*`, `CampaignWorker`. Batch runner is not loaded.

Milestone 1 pipeline is unchanged:

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

CSV import for the API lives in `contact.CsvContactReader` (not `ExcelReader`).

## API layout

```text
                 REST API  /api/v1
                    │
                    ▼
              Controllers  (@Profile api)
                    │
                    ▼
                Services
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
      Repositories  Core     Security (JWT)
          │         Logic    (excel/template/campaign/smtp)
          ▼
      PostgreSQL (Flyway)
```

Campaign execution:

```text
Campaign API → CampaignService → campaign_recipients (PENDING)
    → CampaignWorker (worker package)
    → EmailComposer → EmailMessage → EmailSender → SMTP / dry-run
```

`campaign` must not import `smtp` (`ArchitectureLayeringTest`). The worker may import both.

JPA entities for templates and SMTP accounts live in `mailtemplate` and `smtpaccount` so Milestone 1 `template` / `smtp` packages stay free of web/JPA coupling.

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

Runtime path (CLI): Excel file → `ExcelReader` → `Contact` → `EmailComposer` / `TemplateRenderer` → `EmailMessage` → `EmailSender` → `SmtpEmailSender` (or dry-run).

---

## Module responsibilities

| Module | Owns | Must not own |
| --- | --- | --- |
| **excel** | Read `.xlsx`, headers/aliases, rows → `Contact`, skip/report bad rows | SMTP, templates, sent-log, CLI flags |
| **template** | Load UTF-8 body, render `{{word}}`, template checks | Excel I/O, `JavaMailSender` |
| **smtp** | `EmailSender`, MIME/SMTP, attachment on the message, dry-run vs send | Excel parse, placeholder regex |
| **campaign** | `EmailMessage` / `EmailComposer`; API campaign entities/lifecycle | POI; `com.mailSender.smtp` |
| **worker** | Poll PENDING recipients, send, retries, duplicate claim | HTTP controllers |
| **config** | Bind env/`mail.*`/`spring.mail.*`/`excelmail.*` | Business rules |
| **application** | Start/stop, batch vs skip, test-send, preflight; API profile servlet | Parsing Excel cells |

`MailBody` was not renamed. `SentAddressLog` remains for CLI; API campaigns use `campaign_recipients`.

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

## Constraints

- CLI: `WebApplicationType.NONE`; no database required
- API: explicit `api` profile; worker disabled in `apitest`
- Default: batch off, dry-run on
- Dry-run must not call `JavaMailSender` and must not append the CLI sent-log
- Per-recipient send failures continue; worker isolates failures
- No secrets in git; SMTP passwords encrypted at rest for API accounts
- Profiles: default `development`; `production` has no credentials in the committed file

Operator docs: [README.md](../README.md), [API.md](API.md), [DATABASE.md](DATABASE.md), [SECURITY.md](SECURITY.md).
