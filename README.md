# Corporate Internet Banking (CIB) — Backend

A Spring Boot API for corporate internet banking. Businesses (organizations) get their own accounts and staff, and
money only moves after a second person approves it.

## Features

- **Multi-tenant organizations**: each organization sees only its own users, accounts and transactions. Platform
  super admins see everything.
- **Roles**: `SUPER_ADMIN`, `ADMIN`, `MAKER`, `APPROVER`. Users join by email invitation.
- **Maker–approver transfers**: a maker starts a transfer or payout, and an approver accepts or rejects it. Funds are
  reserved when the transfer starts. Transfers nobody approves expire (48 hours by default), and their reserved funds
  are released.
- **Paystack**:
  - each organization gets a dedicated virtual account that outside parties can pay into
  - accounts can be funded through Paystack checkout
  - payouts go to external bank accounts
  - webhooks are signature-verified and safe to receive more than once
- **Multi-currency** transfers using live exchange rates, with every balance change recorded in a ledger.
- **Concurrency safety**: rows are locked during every balance change, so double approvals and overdrafts can't
  happen.
- **JWT authentication**, with single-use password reset tokens.
- **Scheduled jobs**: daily reports, alerts for stale approvals, transfer expiry and notification delivery.

## Tech stack

Java 17 · Spring Boot 3.3 · Spring Security + JWT · Spring Data JPA · PostgreSQL · OpenFeign · Spring Mail ·
Testcontainers · Spotless (Palantir Java Format) · GitHub Actions

## Running locally

**Requirements:** JDK 17 and Docker. Lombok doesn't compile on much newer JDKs.

1. Start Postgres and Mailpit (Mailpit catches outgoing email locally):

   ```bash
   docker run -d --name cib-postgres -p 5434:5432 \
     -e POSTGRES_DB=cib -e POSTGRES_USER=cib -e POSTGRES_PASSWORD=cib \
     -v cib-pgdata:/var/lib/postgresql/data postgres:16
   docker run -d --name cib-mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
   ```

2. Configure the app. `.env` is git-ignored and loaded automatically:

   ```bash
   cp .env.example .env   # then fill in the values; the comments explain each one
   ```

   For the containers above, use:

   ```properties
   DATABASE_URL=jdbc:postgresql://localhost:5434/cib
   DB_USERNAME=cib
   DB_PASSWORD=cib
   MAIL_HOST=localhost
   MAIL_PORT=1025
   MAIL_SMTP_AUTH=false
   MAIL_SMTP_STARTTLS=false
   ```

3. Build and run:

   ```bash
   ./mvnw spotless:apply package -DskipTests
   java -jar target/corporateInternetBanking-0.0.1-SNAPSHOT.jar
   ```

   The API runs at `http://localhost:1990`, and emails show up at `http://localhost:8025`.

4. Create the first super admin, who then approves organizations and invites everyone else:

   ```bash
   java -jar target/corporateInternetBanking-0.0.1-SNAPSHOT.jar create-super-admin --email=you@example.com
   ```

   [docs/super-admin.md](docs/super-admin.md) covers this command and inviting more super admins.

## API overview

| Area          | Base path               | Highlights                                                           |
|---------------|-------------------------|----------------------------------------------------------------------|
| Auth          | `/auth`                 | login, forgotten password, password reset                            |
| Users         | `/users`                | invitations, registration, profiles                                  |
| Organizations | `/organizations`        | create, approve (provisions the Paystack virtual account), profiles  |
| Accounts      | `/accounts`             | create and view an organization's accounts                           |
| Transactions  | `/transactions`         | initiate, approve/reject, pending list, history                      |
| External      | `/external/transaction` | Paystack funding and payouts                                         |
| Currencies    | `/currencies`           | enable/disable supported currencies                                  |
| Dashboard     | `/dashboard`            | stats for the caller's organization (or the whole platform)          |
| Webhooks      | `/webhook/paystack`     | Paystack events (requires `x-paystack-signature`)                    |

Every endpoint except login, registration, password reset and the webhook needs an `Authorization: Bearer <token>`
header.

## Tests

```bash
./mvnw verify
```

`verify` checks formatting, then runs the unit and integration tests. Integration tests start a throwaway Postgres
with Testcontainers, so Docker must be running. Paystack and email are mocked. The suite covers tenant isolation,
concurrent approvals and balance changes, webhook handling, password reset and super admin creation.

To test against real Paystack test-mode webhooks on your machine, see
[docs/local-paystack-testing.md](docs/local-paystack-testing.md).

## Contributing

- Branch off `dev` with a `feature/`, `fix/`, `chore/`, `docs/` or `test/` prefix, then open a PR into `dev`.
  `dev` is merged into `main` for releases.
- Run `./mvnw spotless:apply` before committing. CI fails on unformatted code.
- CI runs `mvn verify` on every push and PR to `main` and `dev`.
