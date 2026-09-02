# SendEmailService

Sends personalized emails to recipients listed in an Excel file, using Java and Spring Boot Mail.

## SMTP configuration

Do **not** put SMTP passwords in source control. A Gmail **App Password** that was previously hardcoded in this repo should be **revoked and rotated** in Google Account settings, because it may still exist in git history.

Set credentials via environment variables or a local properties file.

### Environment variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `MAIL_USERNAME` | SMTP username | empty |
| `MAIL_PASSWORD` | SMTP password (Gmail App Password) | empty |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_FROM` | From address | SMTP username |
| `MAIL_SUBJECT` | Email subject | configured default |
| `MAIL_EXCEL_FILE_PATH` | Recipients Excel path | empty |
| `MAIL_BODY_FILE_PATH` | Body template text file | empty |
| `MAIL_ATTACHMENT_PATH` | Optional attachment | empty |
| `MAIL_BATCH_ENABLED` | Run send-on-startup batch | `false` |
| `MAIL_DRY_RUN` | Print To + body; skip SMTP | `true` |
| `MAIL_HTML` | Send body as HTML | `false` |
| `MAIL_SENT_LOG_PATH` | File of already-sent addresses | empty |
| `MAIL_SEND_DELAY_MS` | Delay between real sends (ms) | `1000` |

### Local properties file

1. Copy `src/main/resources/application-local.properties.example` to `src/main/resources/application-local.properties`.
2. Fill in username, password, and file paths. That file is gitignored.

## Run

Startup does **not** send mail by default: `mail.batch-enabled` is `false`, and `mail.dry-run` is `true`. Real SMTP runs only when the batch is enabled **and** dry-run is off.

```
mvn spring-boot:run
```

To preview a batch without SMTP (prints each To address and personalized body):

```
MAIL_BATCH_ENABLED=true MAIL_DRY_RUN=true mvn spring-boot:run
```

To send for real (use a test inbox first):

```
MAIL_BATCH_ENABLED=true MAIL_DRY_RUN=false mvn spring-boot:run
```

Real send also requires SMTP username, password, and from-address. If `mail.attachment-path` is set, the file must exist and be readable or the job fails before any message is sent.

Successful To-addresses are appended to `mail.sent-log-path` (when set) and skipped on later runs. Dry-run does not write that log. There is a pause of `mail.send-delay-ms` between real send attempts (not after the last, and not in dry-run).

## Excel and templates

The first row is treated as headers when it includes `email` and `name` (aliases such as `e-mail` / `full_name` are accepted). Other headers become `{{placeholder}}` keys: non-word characters are stripped and the key is lowercased (`Company` → `{{company}}`).

If the first row does not look like headers, column A is email and column B is name.

Blank rows and cells without `@` are skipped. Formula and numeric cells are read via Apache POI `DataFormatter`.

The body file is UTF-8. Placeholders of the form `{{key}}` are replaced per recipient. Built-in keys are `{{name}}` and `{{email}}`. Unknown placeholders are replaced with an empty string.

Example:

```
Hi {{name}},

This message was sent to {{email}} at {{company}}.
```
