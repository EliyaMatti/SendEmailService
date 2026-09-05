---
name: m1-orchestrator
description: Orchestrates ExcelMail Pro Milestone 1 remaining work from M1-016 through M1-035. Use when the user asks to finish Milestone 1, run the M1 orchestrator, continue from M1-016, or complete all remaining DEVELOPMENT_TASKS.md IDs after SMTP/email-model work.
---

# Milestone 1 orchestrator (from M1-016)

Parent agents coordinate. Worker agents implement **one task ID per pass**.

M1-001 through M1-010 are complete. M1-011 through M1-015 exist in code (`EmailSender`, `SmtpEmailSender`, `EmailMessage`, `EmailComposer`, test-send tests). This orchestrator **starts at M1-016** and runs through **M1-035**. Do not reopen discovery (M1-001–M1-003). Do not implement Milestone 2.

## Queue

| ID | Focus |
| --- | --- |
| M1-016 | Centralize configuration (named `mail.*` / sending delay; no magic sleeps) |
| M1-017 | `development` / `production` Spring profiles; no production credentials in git |
| M1-018 | Structured SLF4J logs for the listed events; never log SMTP password |
| M1-019 | Custom exceptions only where they help (`ExcelProcessingException`, etc.) |
| M1-020 | User-facing errors; stack traces stay in logs |
| M1-021 | Excel unit tests |
| M1-022 | Template rendering unit tests |
| M1-023 | Validation tests; ~80% coverage of refactored core (honest numbers) |
| M1-024 | `EmailSender` tests with mocked SMTP; no real mail |
| M1-025 | End-to-end local path with dry-run / mocks |
| M1-026 | Regression: original Excel → SMTP use case still works (dry-run / mocks) |
| M1-027 | README + `docs/` developer docs |
| M1-028 | `docs/EXCEL_FORMAT.md` |
| M1-029 | Unused code cleanup only |
| M1-030 | Dependency review; no unnecessary major upgrades |
| M1-031 | `mvn clean verify` |
| M1-032 | Full test suite green |
| M1-033 | Credential/secret scan |
| M1-034 | Architecture review vs Excel → Contact → template → EmailMessage → EmailSender |
| M1-035 | `docs/NEXT_MILESTONE.md` (document only) |

Coupled pair only if `DEVELOPMENT_TASKS.md` says so. Default: **one ID**.

## Parent loop

1. Read `AGENTS.md`, `DEVELOPMENT_TASKS.md`, `AGENT_WORKFLOW.md`.
2. Pick the first queue ID whose heading is not `[x]`.
3. Launch or perform that ID only.
4. Require: implement → compile → `mvn -q test` → verify without live SMTP → update README/`mail.*` if keys changed → mark `[x]`.
5. Repeat until M1-035 is `[x]`, then write `DEVELOPMENT_TASKS.md` §22 Final Report and §21 Definition of Done.

If blocked: mark `[!]` and stop the queue.

## Worker rules

- Inspect existing Excel → SMTP flow before editing.
- Small diffs. Preserve behavior.
- `mail.batch-enabled=false` in tests; dry-run or mock `JavaMailSender`. Never call real SMTP to verify.
- No secrets in source, commits, or README examples.
- No SaaS/APIs/auth/PostgreSQL/payments (out of scope).

## Stop

Hooks in `.cursor/hooks.json` run `node .cursor/hooks/m1-orchestrator-next.cjs` after `stop` / `generalPurpose` subagent stop. If a hook follow-up arrives, do that ID; do not skip ahead.
