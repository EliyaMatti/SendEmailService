# Security

## Authentication

JWT HMAC (`excelmail.security.jwt-secret` / `APP_JWT_SECRET`). Register and login are public; other `/api/v1/**` routes need `Authorization: Bearer`. Passwords are BCrypt (`password_hash` never appears in JSON).

## Authorization and tenants

The token carries `organizationId`. Services call `TenantService.requireMembership` and load resources by id **and** organization. IDs alone are not enough. Organization A cannot read B’s lists, templates, SMTP accounts, campaigns, recipients, or usage.

## SMTP credentials

Stored as AES-GCM ciphertext plus `key_version`. Key is `APP_ENCRYPTION_KEY` (Base64 128- or 256-bit). APIs never return `password` or `encryptedPassword`. `SmtpConfiguration.toString()` still masks `password=***`. Test endpoint returns a safe success/failure string.

## Secrets

Do not commit `MAIL_PASSWORD`, `DB_PASSWORD`, `APP_JWT_SECRET`, `APP_ENCRYPTION_KEY`, or `application-local.properties` / `.env`. Examples stay placeholders. Actuator does not expose `/actuator/env`. Mail health indicator is disabled so Actuator never probes live SMTP.

## Uploads

Type and size checks (`.xlsx` / `.csv`, `excelmail.upload.max-file-bytes`). Original filenames are stripped to a single path segment. Temp `.xlsx` files are deleted after `ExcelReader.read`.

## Email safety

No SMTP-limit bypass. Worker honors `mail.dry-run`, `mail.send-delay-ms`, `excelmail.limits.*`, and limited retries. Permanent failures (invalid recipient, auth, configuration) are not retried indefinitely. Duplicate sends are prevented by claiming `PENDING → PROCESSING` with a conditional update.

## Logging

Auth, import summaries, campaign lifecycle, and SMTP test outcome are logged without passwords, JWTs, encryption keys, or extra contact PII.
