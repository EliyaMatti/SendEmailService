# Agent workflow — complete `DEVELOPMENT_TASKS.md`

Use this file when implementing ExcelMail Pro Milestone 1 work from `DEVELOPMENT_TASKS.md`. One task (or one tightly coupled pair, e.g. M1-005 + M1-006) per pass. Work phases in order; do not skip discovery for implementation.

## Goal

Close every checkbox in `DEVELOPMENT_TASKS.md` without sending real mail during development. Each closed item must match its bullets and deliverables in `DEVELOPMENT_TASKS.md`.

## Before you start

1. Read `AGENTS.md`, `DEVELOPMENT_TASKS.md`, and this file. The **Suggested order** in `DEVELOPMENT_TASKS.md` is the sequence. Cursor rules in `.cursor/rules/` always apply for mail safety.
2. Pick the first unchecked item in that order. If two IDs are listed together, treat them as one pass.
3. Read the files named under that item. Do not rewrite unrelated classes.
4. Confirm SMTP will not fire: `mail.batch-enabled=false` in tests, or dry-run once H2 exists. Never use a live App Password against a real recipient list while iterating.

## Loop (repeat until `DEVELOPMENT_TASKS.md` is done)

```
1. Select next ID from Suggested order
2. Implement only that ID (plus tests for it)
3. mvn -q test
4. Check off the ID in DEVELOPMENT_TASKS.md
5. Stop or start the next ID — do not bundle unrelated tasks in one change unless asked
```

### Definition of done (every ID)

- Behavior matches the `DEVELOPMENT_TASKS.md` bullets and deliverables for that ID.
- Tests that prove the new behavior live with the change (`M6` is the dedicated test pass; still add focused tests earlier when the code is testable).
- `README.md` updated if you added config keys, defaults, or run behavior (`H2`, `M2`, `M5`, `H5`, extra columns).
- `application.properties` / `MailAppProperties` / `application-local.properties.example` stay in sync for new `mail.*` keys.
- No secrets in source. Do not commit `application-local.properties` or `.env`.
- `DEVELOPMENT_TASKS.md`: change `- [ ]` to `- [x]` (or `[!]` if blocked) for that task.

### Stop conditions (ask the user)

- Need a product choice `DEVELOPMENT_TASKS.md` left open. Document the choice in the task or README.
- Would send mail, rewrite git history, or rotate credentials for **I1** (user must rotate the App Password in Google; agent only documents it).
- Scope beyond the current ID.

## Task playbook

### H2 — Safe default / dry-run

- Default `mail.batch-enabled` to `false`.
- Add `mail.dry-run` (env `MAIL_DRY_RUN`): log/print To + personalized body; do not call `JavaMailSender`.
- Real SMTP only when batch is enabled **and** dry-run is false.
- Update README env table.

### H4 — Fail loud on missing files

- Excel or body path missing/unreadable → throw (or `System.exit` non-zero after log). Do not return empty list / `null` and exit 0.
- Keep skip-when-paths-blank if batch is on but paths unset (current skip message is OK); that is config-not-set, not file-not-found.

### H3 — Excel parsing

- Skip header if A/B look like `email`/`name` (case-insensitive) or row 0 is headers.
- Trim; skip blank and invalid emails (basic `contains @` or similar).
- Use Apache POI `DataFormatter` (and formula evaluator if needed) so formula/numeric cells work.
- Log skipped row numbers.

### H1 — Per-recipient failures

- Catch around each `sendEmail`; continue the loop.
- Summary at end: sent / failed / skipped counts (and optionally lists).
- Missing attachment on a real send: fail that message (or fail fast before the loop if the path is set but unreadable — pick one and document it). Do not abort remaining recipients for a single SMTP error.

### M1 — UTF-8 template

- Read body file with `StandardCharsets.UTF_8` (e.g. `Files.newBufferedReader`).
- Can ship in the same pass as H4 if you touch `readFileContent`.

### M6 — Tests (and tests with earlier IDs)

Add JUnit tests; mock `JavaMailSender`. Cover:

| Area | After ID |
| --- | --- |
| `MailBody.personalize` (missing keys, `$` / `\` in names) | M1 / existing `personalize` |
| Excel header, formulas, invalid email | H3 |
| Extra columns → placeholders | M3 |
| One send failure does not stop later recipients | H1 |
| Attachment missing vs omitted | existing attachment + H1 policy |
| Dry-run does not call send | H2 |
| Missing body/excel file fails | H4 |

Run: `mvn test`. Context load tests must keep batch off.

### H5 — Sent log

- Persist successful To-addresses (file path via `mail.sent-log-path` / env).
- Skip those addresses on later runs; count them as skipped.
- Dry-run must **not** append to the sent log.

### M2 — HTML

- `mail.html` / `MAIL_HTML`; `MimeMessageHelper.setText(body, html)`.
- Default false (plain text).

### M3 — Extra columns

- Row 0 = headers; `email` and `name` required (aliases OK).
- Other headers become placeholder keys (`{{company}}`). Sanitize keys to `\\w+` to match `MailBody` regex, or extend the regex and document it.

### M4 — SMTP check

- Before the send loop, if dry-run is false: require non-blank username, password, and from.
- Fail with a clear message; do not start sending.

### M5 — Rate limit

- `mail.send-delay-ms` (default e.g. 500–1000). Sleep between successful/attempted sends, not after the last.
- Skip delay in dry-run or when delay is 0.

### L1 — pom.xml

- One `spring-boot-maven-plugin`.
- Spotless only as a plugin; one version.
- Remove compile-scope Spotless; drop explicit POI extras if Maven still resolves them transitively.

### L2 — No unused web

- Remove `spring-boot-starter-web` **or** `SpringApplication.setWebApplicationType(NONE)`.
- Tests that assumed a web context must still pass.

### L3 — Logging

- SLF4J on runners/services. No `System.out` / `printStackTrace` on those paths.

### L4 — Immutable placeholders

- `getPlaceholders()` returns `Map.copyOf` or `Collections.unmodifiableMap` (copy if the map is mutated at build time).

### I1 — Credential rotation

- Do not put passwords in git.
- Remind the user to revoke any App Password that was ever committed.
- Do not rewrite git history unless the user explicitly asks.

## Constraints

- Do not send mail as a way to “verify” unless the user provides a test inbox and dry-run is off.
- Do not expand into HTTP APIs, UI, or new frameworks.
- Do not commit `.env` or `application-local.properties`.
- Prefer small diffs. Match existing Java style (Spotless / Google Java Format if you run the plugin).

## After the last checkbox

1. `mvn test` is green.
2. `DEVELOPMENT_TASKS.md` Milestone 1 tasks all `[x]` (or `[!]` with explanation).
3. README describes dry-run, batch default, Excel columns, and new env vars.
4. Summarize what shipped vs what the user must still do (I1).
