# Database

PostgreSQL is the API database (`SPRING_PROFILES_ACTIVE=api`). CLI mode does not use a DataSource.

Schema is owned by Flyway under `src/main/resources/db/migration/`. SQL is portable for H2 `MODE=PostgreSQL` tests: `UUID`, `TIMESTAMP WITH TIME ZONE`, `TEXT` (not JSONB), no `gen_random_uuid()`.

## Migrations

| Version | Tables |
| --- | --- |
| V1 | `users` |
| V2 | `organizations` |
| V3 | `organization_members` |
| V4 | `contact_lists` |
| V5 | `contacts` (`metadata_json` TEXT) |
| V6 | `email_templates` |
| V7 | `smtp_accounts` (`encrypted_password`, `key_version`) |
| V8 | `campaigns` |
| V9 | `campaign_recipients` |
| V10 | `usage_records` (`usage_date`) |

## Tenant model

Every business row has `organization_id`. Access is `findByIdAndOrganizationId` after JWT membership check. Guessing another tenant’s UUID returns 404.

## Indexes (query-backed)

- `users.email` unique + index
- membership org/user
- `contacts(organization_id, email)`, `contacts(contact_list_id)`
- `campaigns(organization_id, status)`
- `campaign_recipients(campaign_id, status)`
- `usage_records(organization_id, usage_date)` unique

## Constraints

NOT NULL, FKs, CHECKs on status enums, unique `(contact_list_id, email)` and `(campaign_id, contact_id)`. Contacts cascade when a list is deleted. Recipients cascade when a campaign is deleted.

## Transactions

Register (user + org + member), contact import replace, campaign start/queue, recipient claim (`UPDATE … WHERE status = PENDING`), usage increments.

## Test database

`application-apitest.properties` uses in-memory H2 PostgreSQL mode + Flyway. Optional Testcontainers Postgres is not required while migrations stay portable.
