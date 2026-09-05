# Next milestone (Milestone 3 — frontend)

Milestone 2 (SaaS backend) is implemented. This file is **documentation only** for Milestone 3. **Do not implement a UI in Milestone 2.**

CLI Excel → SMTP from Milestone 1 remains: `WebApplicationType.NONE` by default, `mail.batch-enabled=false`, `mail.dry-run=true`. The API profile is additive.

## What exists after Milestone 2 (reuse)

- REST `/api/v1/` with JWT register/login/me
- Organizations and membership
- Contact lists, Excel/CSV import
- Templates (`TemplateValidator` / `TemplateRenderer`)
- Encrypted SMTP accounts + mocked test
- Campaigns + `CampaignWorker` (`EmailComposer` + `EmailSender`)
- Usage read API
- OpenAPI and safe Actuator

## Milestone 3 should address (UI only)

```text
React/Next.js frontend
Login UI
Dashboard
Contact management UI
Excel upload UI
Template editor
SMTP configuration UI
Campaign wizard
Campaign monitoring
Campaign reports
```

Do **not** implement billing, tracking pixels, spam-bypass, or production Kubernetes in Milestone 3 unless a later document says so.

## Integration notes for the frontend

- Base path `/api/v1/`
- Store JWT from register/login; send `Authorization: Bearer`
- Envelope `{ success, data, error }`
- Never display or log SMTP passwords (write-only)
- Paginate lists
- Campaign start does not wait for all sends; poll `GET /api/v1/campaigns/{id}`
- Dry-run remains the safe default for non-prod API processes
