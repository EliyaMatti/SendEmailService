# Agent start — SendEmailService

Spring Boot CLI that reads recipients from Excel, fills a `{{placeholder}}` body, and sends SMTP mail (optional attachment) on startup.

## Start here

1. Read `TASKS.md` (what to build) and `AGENT_WORKFLOW.md` (how to close each ID).
2. Follow **Suggested order** in `TASKS.md`. One ID per pass (H4+H3 together).
3. Project rules live in `.cursor/rules/`. Follow them; do not send real mail while implementing.

## Do

- Implement the next unchecked High/Medium/Low item; check it off in `TASKS.md` when done.
- Keep `mail.*` keys in sync: `application.properties`, `MailAppProperties`, `application-local.properties.example`, README.
- Add or extend tests with the change. Run `mvn test`. Context tests must keep batch off.
- Use env vars or gitignored `application-local.properties` for SMTP secrets.

## Do not

- Commit `application-local.properties`, `.env`, or passwords.
- Verify by sending to a real list unless the user names a test inbox and dry-run is off.
- Add HTTP APIs, UI, or extra frameworks (L2: this is a batch/CLI app).
- Rewrite git history or rotate credentials yourself (I1 is the user’s Google Account step).

## Layout

| Path | Role |
| --- | --- |
| `BatchMailRunner` | Startup batch |
| `ReadFromExcel` | Recipients |
| `MailBody` | Template + send loop |
| `EmailService` | SMTP |
| `MailBodyAttachment` | Optional file |
| `MailAppProperties` | `mail.*` config |
