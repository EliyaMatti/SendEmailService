# Agent instructions — Milestone 1

Read [`DEVELOPMENT_TASKS.md`](DEVELOPMENT_TASKS.md) completely and execute **Milestone 1 only** (tasks **M1-001** through **M1-035**).

Then read [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md) for the per-task loop, definition of done, and implementation playbooks. Cursor rules in [`.cursor/rules/`](.cursor/rules/) always apply (especially mail safety).

## Start here

1. Follow the **Suggested order** at the bottom of `DEVELOPMENT_TASKS.md`.
2. **Discovery first:** complete M1-001 → M1-003 (and their `docs/` deliverables) before implementation tasks (M1-005+). Do not skip discovery.
3. Pick the first incomplete task in that order. One task per pass unless `DEVELOPMENT_TASKS.md` lists a tightly coupled pair (e.g. M1-005 + M1-006).
4. Before modifying code, inspect the repository and understand the existing Excel → SMTP workflow.

## Agent Operating Rules

Follow every rule in `DEVELOPMENT_TASKS.md` §1 (Inspect before modifying, Preserve existing behavior, Small changes only, Never expose credentials).

## Per-task workflow

For each task ID:

1. Mark it **in progress** in `DEVELOPMENT_TASKS.md` (add `[~]` to the task heading or note progress in the section).
2. Implement the smallest safe change that satisfies that ID’s bullets and deliverables.
3. Run `mvn -q test`.
4. Verify behavior (no real SMTP; use dry-run or mocked `JavaMailSender` in tests).
5. Update `README.md` and `mail.*` config if you changed keys, defaults, or run behavior.
6. Mark the task **complete** in `DEVELOPMENT_TASKS.md` only after successful verification.
7. If blocked, mark `[!]` and explain the blocker.

Do **not** mark a task complete merely because code was written. Completion requires: implementation → compilation → tests → verification → documentation (`DEVELOPMENT_TASKS.md` §23).

## Constraints

- Do **not** implement Milestone 2 or any SaaS functionality (APIs, auth, PostgreSQL, payments, etc.). See `DEVELOPMENT_TASKS.md` “Explicitly OUT OF SCOPE”.
- Do **not** rewrite the project unnecessarily. Preserve the existing working Excel → SMTP functionality.
- Do **not** use real SMTP credentials or send mail to verify changes.
- Do **not** commit secrets (passwords, API keys, tokens). Do not commit `application-local.properties` or `.env`.
- Do **not** bypass SMTP provider limits or add spam/abuse-enabling functionality.
- For **I1** (credential rotation): remind the user to revoke any leaked App Password in Google; do not rewrite git history unless they explicitly ask.

## Milestone complete

Milestone 1 is done only when:

- All tasks M1-001 through M1-035 are complete (or `[!]` with explanation).
- The **Definition of Done** checklist in `DEVELOPMENT_TASKS.md` §21 is satisfied.
- `mvn test` is green.

## Final Agent Report

When all tasks are complete, produce the report required by `DEVELOPMENT_TASKS.md` §22:

- Completed tasks
- Files changed
- Architecture changes (before/after)
- Test results (total / passed / failed / skipped / coverage)
- Security confirmation (credentials removed, no secrets committed, env-based config)
- Known issues
- Milestone 2 readiness (YES/NO and blockers)

| Path | Role |
| --- | --- |
| `BatchMailRunner` | Startup batch |
| `ReadFromExcel` | Recipients |
| `MailBody` | Template + send loop |
| `EmailService` | SMTP |
| `MailBodyAttachment` | Optional file |
| `MailAppProperties` | `mail.*` config |

## Cloud Agent environment

- Dependencies: `mvn test` (batch stays off in context tests).
- Dry-run e2e (no SMTP secrets): set `MAIL_BATCH_ENABLED=true`, `MAIL_DRY_RUN=true`, plus `MAIL_EXCEL_FILE_PATH` and `MAIL_BODY_FILE_PATH` to sample files, then `mvn spring-boot:run`.
- Real sends need `MAIL_USERNAME` / `MAIL_PASSWORD` (or gitignored `application-local.properties`); never commit credentials.
Begin with **M1-001**.
