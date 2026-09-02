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

Excel columns: email in column A, name in column B.

The body file is a template. Placeholders of the form `{{key}}` are replaced per recipient. Built-in keys are `{{name}}` and `{{email}}`. Unknown placeholders are replaced with an empty string.

Example:

```
Hi {{name}},

This message was sent to {{email}}.
```
