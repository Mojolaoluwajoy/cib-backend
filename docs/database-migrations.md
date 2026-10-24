# Database migrations (Flyway)

The schema is created and changed only by the SQL files in `src/main/resources/db/migration`. Flyway runs any new
ones at startup, in version order, and records each in the `flyway_schema_history` table. Hibernate no longer changes
the schema (`ddl-auto=validate`): it only checks at startup that the entities match the tables, and the app refuses to
start if they don't.

| File | What it does |
|---|---|
| `V1__baseline.sql` | The schema as it was when Flyway was introduced |
| `V2__seed_currencies.sql` | Inserts the supported currencies (all inactive) |
| `V3__index_foreign_keys_and_status.sql` | Indexes for organization lookups, the ledger and the approval jobs |

## Changing the schema

1. Change the entity.
2. Add the next file, e.g. `V4__add_account_nickname.sql`:
   ```sql
   alter table account add column nickname varchar(100);
   ```
3. Run the tests (`./mvnw verify`). They start an empty Postgres, run every migration and validate the result, so a
   migration that doesn't match the entities fails the build.

Rules:

- **Never edit a migration that has already run** anywhere. Flyway checksums each file and stops the app if one
  changed. Fix mistakes with a new migration.
- Version numbers must increase. Two branches that both add `V4` conflict, so the second one to merge renumbers.
- **Adding a value to an enum needs a migration.** Enum columns have a check constraint listing the allowed values
  (e.g. `transaction.status`). Without updating it, inserts with the new value fail:
  ```sql
  alter table transaction drop constraint transaction_status_check;
  alter table transaction add constraint transaction_status_check
      check (status in ('PENDING','PENDING_APPROVAL','APPROVED','SUCCESS','FAILED','REVERSED','REJECTED','EXPIRED','NEW'));
  ```

## Existing databases

A database created before Flyway (by Hibernate's old `ddl-auto=update`) already has the V1 tables. On first start,
`spring.flyway.baseline-on-migrate` records it as version 1 without running V1, then applies V2 onwards.

This assumes the database really matches V1. One that was created from much older code may be missing tables or
columns; schema validation then stops the app with a message naming what's missing. Either add it by hand or, if the
data doesn't matter, start from an empty database.

A fresh database (a new deployment, or the integration tests) simply runs every migration from V1.
