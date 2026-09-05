# API (`/api/v1/`)

ExcelMail Pro Milestone 2 REST API. Responses use `ApiResponse`:

```json
{ "success": true, "data": {} }
```

```json
{ "success": false, "error": { "code": "CAMPAIGN_NOT_FOUND", "message": "The campaign was not found." } }
```

Authenticate with `Authorization: Bearer <jwt>` except register, login, OpenAPI, and actuator health/info.

## Auth

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Creates user + organization + OWNER membership. Body: `name`, `email`, `password` (min 8). |
| POST | `/api/v1/auth/login` | Same credentials. |
| GET | `/api/v1/auth/me` | Current user. Never returns `passwordHash`. |

Token payload includes `userId`, `organizationId`, `role`.

## Contact lists

| Method | Path |
| --- | --- |
| POST | `/api/v1/contact-lists` |
| GET | `/api/v1/contact-lists` |
| GET | `/api/v1/contact-lists/{id}` |
| DELETE | `/api/v1/contact-lists/{id}` |
| GET | `/api/v1/contact-lists/{id}/contacts` |
| POST | `/api/v1/contact-lists/{id}/upload` |

Upload multipart field `file`: `.xlsx` (Milestone 1 `ExcelReader` as-is) or `.csv` (separate parser). Summary: `totalRows`, `valid`, `invalid`, `duplicates`, `errors` like `Row 17: Invalid email format` (no extra PII).

## Templates

| Method | Path |
| --- | --- |
| POST | `/api/v1/templates` |
| GET | `/api/v1/templates` |
| GET | `/api/v1/templates/{id}` |
| PUT | `/api/v1/templates/{id}` |
| DELETE | `/api/v1/templates/{id}` |

Optional `contactListId` on create/update validates `{{placeholders}}` against imported columns (`TemplateValidator`).

## SMTP

| Method | Path |
| --- | --- |
| POST | `/api/v1/smtp` |
| GET | `/api/v1/smtp` |
| GET | `/api/v1/smtp/{id}` |
| DELETE | `/api/v1/smtp/{id}` |
| POST | `/api/v1/smtp/{id}/test` |

Password is write-only. Test connection is mocked in automated tests; never put App Passwords in CI.

## Campaigns

| Method | Path |
| --- | --- |
| POST | `/api/v1/campaigns` |
| GET | `/api/v1/campaigns` |
| GET | `/api/v1/campaigns/{id}` |
| POST | `/api/v1/campaigns/{id}/start` |
| POST | `/api/v1/campaigns/{id}/pause` |
| POST | `/api/v1/campaigns/{id}/resume` |
| POST | `/api/v1/campaigns/{id}/cancel` |

Start validates list, template, SMTP, recipient cap, and daily limits, then sets `RUNNING`. Sending happens in `CampaignWorker`, not on the request thread.

## Usage

`GET /api/v1/usage` — paginated daily counts plus campaign/contact totals for the caller’s organization.

## Pagination

Spring Data: `?page=0&size=20`. Envelope `data.items`, `page`, `size`, `totalItems`, `totalPages`.

## OpenAPI

`/v3/api-docs`, `/swagger-ui.html` (api profile).
