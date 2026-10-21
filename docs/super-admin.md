# Creating super admins

Super admins run the platform (approving organizations, managing currencies). They are never created from
configuration or seed data:

- **The first one** is created by whoever operates the server, with the `create-super-admin` command.
- **Every later one** is invited by an existing super admin through the normal invitation email.

## The first super admin: `create-super-admin`

The command uses the same configuration as the server (database, `.env` / environment variables), creates the
account and exits. It doesn't open an HTTP port or run scheduled jobs, so it's safe to run next to a live server.
The password is typed at a prompt and never stored in shell history, env vars or logs.

```bash
# Running the jar directly (Java 17)
java -jar target/corporateInternetBanking-0.0.1-SNAPSHOT.jar create-super-admin \
  --email=you@example.com --first-name=Ada --last-name=Lovelace

# Inside an already running app container (reuses its configuration)
docker exec -it cib-app java -jar app.jar create-super-admin --email=you@example.com

# Any Docker host, from the image built by the Dockerfile; pass the same env vars as the server
docker run -it --rm --env-file prod.env <image> create-super-admin --email=you@example.com
```

Without a terminal (e.g. in automation) the password is read from the first line of stdin:

```bash
docker exec -i cib-app java -jar app.jar create-super-admin --email=you@example.com < password.txt
```

Rules: a valid email, a password of at least 10 characters, and an email no other user has. Exit code `0` means
created; anything else prints the reason.

## More super admins: invitation

Log in as a super admin and call `POST /users/invitation` with:

```json
{ "userEmail": "colleague@example.com", "role": "SUPER_ADMIN" }
```

The invitee receives the usual registration email and sets their own password. Only super admins can invite
super admins; organization admins can only invite `ADMIN`, `MAKER` or `APPROVER` into their own organization.
