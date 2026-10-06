# LifeCapsule — backend

Spring Boot API for a personal family-tree app: people, relationships, family access/invitations, and a photo/video gallery per person.

Frontend lives in a separate project: `../lifecapsule-front-system` (React + Vite).

## Prerequisites

- Java 17
- PostgreSQL running locally (or reachable), with a database created for the app
- Node.js (only needed for the separate frontend project)

## Configuration

All settings live in [`src/main/resources/application.yml`](src/main/resources/application.yml), with sensible local-dev defaults baked in. Override any of them with environment variables — nothing needs to change in the file itself for local development.

| Env var | Default | What it controls |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/lifecapsule` | Postgres connection URL |
| `DB_USERNAME` | `postgres` | Postgres username |
| `DB_PASSWORD` | `123` | Postgres password |
| `JWT_SECRET` | (a generated default in the file) | Signing key for auth tokens. **Change this and keep it secret** if this app is ever exposed beyond your own machine — anyone with the key can forge login tokens. |
| `SHOW_SQL` | `false` | Log every SQL statement Hibernate runs |
| `APP_STORAGE_ROOT` | `./uploads` | Folder where uploaded photos/videos are saved on disk (created automatically if missing) |
| `APP_ADMIN_ENABLED` | `true` | Whether a default admin account is created on first startup |
| `APP_ADMIN_USERNAME` | `admin` | Default admin username |
| `APP_ADMIN_PASSWORD` | `Admin123!` | Default admin password — **change this** for anything beyond local testing |
| `APP_ADMIN_EMAIL` | `admin@lifecapsule.local` | Default admin email |

Access tokens live 1 hour and refresh tokens 30 days (the frontend renews silently). Tokens identify the user by id, so renaming an account keeps the session; changing or resetting a password revokes every token issued before it.

The admin account (`Role.ADMIN`) can see and manage every family, and can reset any user's password (see below) — useful since there's no self-service "forgot password" email flow in this project.

## Running locally

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8085`. On first run, Hibernate creates the schema (`ddl-auto: update` — it only adds what's missing, it never drops or wipes existing tables or data) and the admin account is seeded if enabled.

⚠️ Never set `ddl-auto` to `create` or `create-drop` once you have real data — either wipes the entire schema (and every family, person, and photo in it) on the next restart.

Run the test suite:

```bash
./mvnw test
```

Tests run under the `test` profile against a separate `lifecapsule_test` database (override with `TEST_DB_URL`), which Hibernate recreates on every run — create it once with `createdb lifecapsule_test`. They never touch the development database.

Login is throttled: 5 wrong passwords for one username within 15 minutes lock that username for 15 minutes (HTTP 429).

## API testing

A Postman collection and local environment are in [`postman/`](postman/):

- `lifecapsule.postman_collection.json`
- `lifecapsule.local.postman_environment.json`

Import both, run **Auth → Login** first (it stores the token automatically), then explore the rest. Covers auth, families, persons, relationships, family access/invitations, media upload/download, and admin password reset.

## Media storage

Photos and videos are stored on local disk under `APP_STORAGE_ROOT` (default `./uploads`), organized as `uploads/{familyId}/{personId}/{uuid}.{ext}`. Files are served only through authenticated API endpoints (`GET /families/{familyId}/persons/{personId}/media/{mediaId}/file`) — there is no public/static file serving, so private family photos stay private. Deleting a person or a family also deletes their files from disk, not just the database rows.

If you ever move the app to another machine or container, remember to move the `uploads` folder along with the database — it isn't part of the Postgres dump.

## Forgot password

There's no email-based password reset (no SMTP is configured — overkill for a personal project). Instead, an admin can reset anyone's password directly:

```
PATCH /admin/users/{username}/password
Authorization: Bearer <admin token>
{ "newPassword": "..." }
```

See the "Admin" folder in the Postman collection.
