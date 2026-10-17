# SportsAura User Service

The account service of the SportsAura platform (where users hire sports trainers).
It handles **registration**, **login** and **user profiles**. Other SportsAura services
trust the login token (JWT) this service issues.

- How the code is organised: [STRUCTURE.md](STRUCTURE.md)
- Interactive API docs once the app is running: <http://localhost:8081/swagger-ui.html>

## Contents

1. [Tech stack](#tech-stack)
2. [What you need installed](#what-you-need-installed)
3. [Quick start with Docker](#quick-start-with-docker)
4. [Run on your machine for development](#run-on-your-machine-for-development)
5. [Environment variables](#environment-variables)
6. [Using the API](#using-the-api)
7. [Code quality: lint and format](#code-quality-lint-and-format)
8. [Editor setup (red and yellow lines)](#editor-setup-red-and-yellow-lines)
9. [Tests](#tests)
10. [Database migrations](#database-migrations)
11. [Useful commands](#useful-commands)
12. [Troubleshooting](#troubleshooting)

## Tech stack

| Area | Choice |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator) |
| Database | MySQL 8.4 |
| Migrations | Flyway |
| Login tokens | JWT (JJWT), passwords hashed with BCrypt |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven (wrapper included: `mvnw`) |
| Formatter | Spotless + google-java-format (the Java equivalent of Prettier) |
| Linter | Checkstyle (the Java equivalent of ESLint) |
| Containers | Docker, Docker Compose |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |

## What you need installed

| Tool | Needed for | Check |
|---|---|---|
| Docker Desktop | Running the app and the database | `docker --version` |
| JDK 25 | Running, testing and editing the code outside Docker | `java -version` |
| Git | Version control | `git --version` |

You do **not** need to install Maven. Use the included wrapper: `./mvnw` (macOS/Linux)
or `mvnw.cmd` (Windows). The examples below use `./mvnw`; on Windows type `mvnw.cmd` instead.

If you only want to run the app, Docker alone is enough.

## Quick start with Docker

This runs the service and its MySQL database with one command.

```bash
# 1. Create your environment file from the sample
cp .env.example .env            # Windows PowerShell: Copy-Item .env.example .env

# 2. Open .env and change the passwords and JWT_SECRET

# 3. Build and start everything
docker compose up --build
```

When you see `Started UserServiceApplication`, the service is ready:

- Health check: <http://localhost:8081/actuator/health>
- Swagger UI: <http://localhost:8081/swagger-ui.html>

Stop it with `Ctrl+C`, or `docker compose down`. Your data is kept in a Docker volume.
To wipe the database as well: `docker compose down -v`.

## Run on your machine for development

Run only the database in Docker and the app from your editor or terminal, so code
changes are quick to try.

```bash
# 1. Create .env (once)
cp .env.example .env

# 2. Start only MySQL (published on localhost:3307 by default)
docker compose up -d user-db

# 3. Start the app
./mvnw spring-boot:run
```

The app reads `.env` from the project root automatically. You can also press **Run**
on `UserServiceApplication.java` in your editor.

## Environment variables

All configuration comes from environment variables. For local work they live in `.env`
(never committed). [`.env.example`](.env.example) is the sample to copy.

| Variable | Required | Default | Meaning |
|---|---|---|---|
| `SERVER_PORT` | no | `8081` | Port the service listens on |
| `DB_HOST` | no | `localhost` | MySQL host. Docker Compose sets it to `user-db` for the app container |
| `DB_PORT` | no | `3306` | MySQL port. The sample uses `3307`, the port Docker publishes MySQL on |
| `DB_NAME` | no | `sportsaura_user` | Database (schema) name |
| `DB_USERNAME` | **yes** | none | MySQL user the app logs in with |
| `DB_PASSWORD` | **yes** | none | Password of that user |
| `DB_ROOT_PASSWORD` | Docker only | none | MySQL root password, used by Docker Compose to create the database |
| `JWT_SECRET` | **yes** | none | Secret that signs login tokens. At least 32 characters. Generate with `openssl rand -base64 48` |
| `JWT_EXPIRATION_MINUTES` | no | `15` | How long an access token stays valid |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | no | `30` | How long a refresh token stays valid; after that the user must log in again |

The app refuses to start if a required value is missing or `JWT_SECRET` is too short.

## Using the API

Base URL: `http://localhost:8081`. All request and response bodies are JSON.

| Method | URL | Who can call it | What it does |
|---|---|---|---|
| POST | `/api/v1/auth/register` | anyone | Create a `USER` or `TRAINER` account, returns a token |
| POST | `/api/v1/auth/login` | anyone | Log in, returns a token |
| POST | `/api/v1/auth/refresh` | anyone with a refresh token | Swap a refresh token for a new pair of tokens |
| POST | `/api/v1/auth/logout` | anyone with a refresh token | Log out: the refresh token stops working |
| GET | `/api/v1/users/me` | logged-in user | Get my profile |
| PUT | `/api/v1/users/me` | logged-in user | Update my name and phone |
| PATCH | `/api/v1/users/me/password` | logged-in user | Change my password |
| GET | `/api/v1/users` | `ADMIN` | List all users (paged: `?page=0&size=20&sort=createdAt,desc`) |
| GET | `/api/v1/users/{id}` | `ADMIN` | Get any user by id |
| GET | `/actuator/health` | anyone | Health check |

### 1. Register

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Asha","email":"asha@example.com","password":"secret-password","phone":"+919876543210","role":"TRAINER"}'
```

`phone` and `role` are optional. `role` is `USER` (default) or `TRAINER`.

Response (`201 Created`):

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "q3Jx9v...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshExpiresInSeconds": 2592000,
  "user": {
    "id": 1,
    "name": "Asha",
    "email": "asha@example.com",
    "phone": "+919876543210",
    "role": "TRAINER",
    "active": true,
    "createdAt": "2026-10-12T14:39:45.606679Z",
    "updatedAt": "2026-10-12T14:39:45.606679Z"
  }
}
```

### 2. Log in

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"asha@example.com","password":"secret-password"}'
```

The response has the same shape as register.

### 3. Call a protected endpoint

Send the token in the `Authorization` header:

```bash
curl http://localhost:8081/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>"
```

### 4. Update profile and change password

```bash
curl -X PUT http://localhost:8081/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Asha Rao","phone":"+919876543210"}'

curl -X PATCH http://localhost:8081/api/v1/users/me/password \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"secret-password","newPassword":"a-new-password"}'
```

Changing the password logs the user out on every device: all their refresh tokens stop working.

### 5. Stay logged in: refresh and logout

Login and register return two tokens:

| Token | Lives | Used for |
|---|---|---|
| `accessToken` | 15 minutes | Sent as `Authorization: Bearer ...` on every request |
| `refreshToken` | 30 days | Sent only to `/auth/refresh` to get a new pair, and to `/auth/logout` |

When the access token expires (the API answers `401`), the app gets a new pair:

```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

The response has the same shape as login. **Save the new refresh token**: each refresh
token works only once. If an already-used refresh token is sent again, the service assumes it
was stolen and logs that user out everywhere.

To log out:

```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

Returns `204`. The refresh token stops working at once; the current access token keeps working
until it expires (at most 15 minutes), so the app should also delete it.

The database stores only a SHA-256 hash of each refresh token, never the token itself.

### Errors

Every error has the same shape (RFC 9457 "problem details"):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/auth/register",
  "errors": {
    "email": "Email must be a valid email address"
  }
}
```

| Status | Meaning |
|---|---|
| 400 | Invalid input (`errors` lists the fields) or a broken business rule |
| 401 | Missing, invalid or expired access token; wrong email/password; or a refresh token that is unknown, expired or already used |
| 403 | Logged in, but your role is not allowed to do this |
| 404 | Not found |
| 409 | Email already registered |
| 500 | Unexpected server error (details are in the server log, not the response) |

### Creating the first admin

Admins cannot be created through the API. Register a normal account, then promote it
in the database:

```bash
docker compose exec user-db mysql -uroot -p sportsaura_user \
  -e "UPDATE users SET role='ADMIN' WHERE email='you@example.com';"
```

Log in again to get a token that carries the new role.

### Swagger UI

Open <http://localhost:8081/swagger-ui.html>, call **register** or **login**, copy the
`accessToken`, click **Authorize** and paste it. You can then try every endpoint from the browser.

## Code quality: lint and format

Two tools keep the code consistent. Both run automatically in `./mvnw verify` and in CI,
so a pull request with problems fails.

| Tool | Like | What it does | Command |
|---|---|---|---|
| Spotless | Prettier | Rewrites code to one standard format (Google Java Style) | `./mvnw spotless:apply` |
| Checkstyle | ESLint | Finds bad patterns: unused imports, missing braces, bad names, empty catch blocks | `./mvnw checkstyle:check` |

```bash
./mvnw spotless:apply     # fix formatting
./mvnw spotless:check     # only report formatting problems
./mvnw checkstyle:check   # run the linter
./mvnw verify             # compile + tests + format check + lint (run before every push)
```

Lint rules live in [`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml).
Rules marked `error` fail the build; rules marked `warning` are only printed.

## Editor setup (red and yellow lines)

The repo ships VS Code settings so problems show up while you type.

1. Install JDK 25.
2. Open this folder in VS Code.
3. When VS Code asks **"Do you want to install the recommended extensions?"**, choose **Install**
   (or run *Extensions: Show Recommended Extensions*).
4. Wait for the Java project to finish importing (status bar, bottom right).

What you get:

| You see | It comes from | Meaning |
|---|---|---|
| Red line | Java compiler | The code does not compile |
| Red line | Checkstyle rule with severity `error` | Lint error; `./mvnw verify` will fail |
| Yellow line | Checkstyle rule with severity `warning`, Java compiler warning, or SonarQube for IDE | Worth fixing, does not fail the build |
| Auto-format on save | google-java-format | Same result as `./mvnw spotless:apply` |

All problems are also listed in the **Problems** panel (`Ctrl+Shift+M`).

Using IntelliJ IDEA instead: install the **CheckStyle-IDEA** and **google-java-format**
plugins and point CheckStyle-IDEA at `config/checkstyle/checkstyle.xml`.

## Tests

```bash
./mvnw test       # tests only
./mvnw verify     # tests + format check + lint
```

**Docker must be running.** Integration tests start a real MySQL 8.4 in Docker (Testcontainers),
run every Flyway migration on it, and check the tables match the code, exactly like production.
No `.env` file is needed; the MySQL container is created and removed automatically.

- `AuthFlowIntegrationTest` and `RefreshTokenFlowIntegrationTest` start the whole app on MySQL
  and call the real endpoints.
- `FlywayMigrationIntegrationTest` checks every migration applies, the tables exist, and the
  constraints (unique email, delete cascade) work.
- `*ServiceTest` and `JwtServiceTest` are fast unit tests of the business logic; they need no Docker.

No JDK installed? Run the same thing in Docker. The extra `docker.sock` mount lets the tests
start their MySQL container:

```bash
docker run --rm -v "${PWD}:/app" -v sportsaura-m2:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -w /app maven:3.9-eclipse-temurin-25 mvn -B verify
```

## Database migrations

Tables are created and changed by **Flyway**, not by Hibernate. Migrations are SQL files in
`src/main/resources/db/migration/` and run automatically when the app starts.

- File name: `V<number>__<short_description>.sql`, for example `V2__add_avatar_url_to_users.sql`.
- **Never edit a migration that has already run.** To change the schema, add a new file.
- When you add a column, update the matching entity in the same commit. The app checks at
  startup that tables and entities match (`ddl-auto: validate`) and refuses to start if not.
- This service only owns its own database. Other SportsAura services have their own
  migrations in their own repos and refer to a user only by id.

## Useful commands

| Task | Command |
|---|---|
| Start everything | `docker compose up --build` |
| Start only the database | `docker compose up -d user-db` |
| Stop | `docker compose down` |
| Stop and delete the database | `docker compose down -v` |
| See app logs | `docker compose logs -f user-service` |
| Open a MySQL shell | `docker compose exec user-db mysql -uroot -p sportsaura_user` |
| Start with phpMyAdmin (database browser on <http://localhost:8090>) | `docker compose -f docker-compose.yml -f docker-compose.tools.yml up -d` |
| Stop everything including phpMyAdmin | `docker compose -f docker-compose.yml -f docker-compose.tools.yml down` |
| Run the app locally | `./mvnw spring-boot:run` |
| Fix formatting | `./mvnw spotless:apply` |
| Full check before pushing | `./mvnw verify` |
| Build the JAR | `./mvnw package` (output: `target/user-service.jar`) |

## Troubleshooting

| Problem | Cause and fix |
|---|---|
| `Could not resolve placeholder 'JWT_SECRET'` (or `DB_USERNAME`) | No `.env` file. Copy `.env.example` to `.env` |
| `JWT_SECRET must be at least 32 characters` | Use a longer secret |
| `Communications link failure` | MySQL is not running or `DB_HOST`/`DB_PORT` is wrong. Run `docker compose up -d user-db`; from your machine the port is `3307` |
| `Access denied for user` | `DB_USERNAME`/`DB_PASSWORD` do not match the database. If you changed them after the first start, run `docker compose down -v` and start again |
| `Schema-validation: missing column` | An entity changed without a migration. Add a `V<n>__*.sql` file |
| `Found non-empty schema(s) ... but no schema history table` | The database has tables from before Flyway was added. Use a fresh database (`docker compose down -v`) |
| `Port 8081 is already in use` | Change `SERVER_PORT` in `.env` |
| `./mvnw verify` fails on `spotless:check` | Run `./mvnw spotless:apply` and commit the result |
| No red/yellow lines in VS Code | Install JDK 25 and the recommended extensions, then run *Java: Clean Java Language Server Workspace* |
| Tests fail with `Could not find a valid Docker environment` | Docker Desktop is not running. Start it and run the tests again |

---

**Author:** Aarif Alam
