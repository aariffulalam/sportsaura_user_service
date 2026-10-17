# Project structure

This file explains where everything in the user service lives and why.
Read the [short version](#the-short-version) first; the rest goes folder by folder.

## The short version

A request travels through the code in one direction:

```
HTTP request
    |
    v
security/     Is there a valid login token? Who is this?
    |
    v
controller/   Which URL is this? Is the JSON body valid?
    |
    v
service/      The business rules (the actual work)
    |
    v
repository/   Read or write the database
    |
    v
MySQL (tables created by db/migration/*.sql)
```

On the way back, the `mapper/` turns database rows (`entity/`) into safe JSON (`dto/response/`),
and if anything goes wrong, `exception/` turns the error into one standard JSON error.

Three rules hold the structure together:

1. **Each layer only talks to the one below it.** Controllers never touch repositories.
2. **Entities never leave the service layer.** The API accepts and returns DTOs only,
   so a database column (like the password hash) can never leak into a response by accident.
3. **Business rules live in `service/` only.** Controllers and repositories stay thin.

## Full tree

```
sportsaura_user_service/
├── .editorconfig                  Basic editor rules (indent, line endings) for any editor
├── .env.example                   Sample environment file; copy to .env
├── .gitattributes                 Forces LF line endings so Windows/macOS/Linux agree
├── .gitignore                     Files git must not track (.env, target/, ...)
├── .dockerignore                  Files Docker must not copy into the image
├── .github/workflows/ci.yml       CI: runs "mvn verify" on every push and pull request
├── .mvn/                          Maven wrapper settings and JVM flags for the formatter
├── .vscode/
│   ├── extensions.json            Extensions VS Code offers to install
│   └── settings.json              Turns on lint lines and format-on-save
├── config/checkstyle/checkstyle.xml   Lint rules (errors = red, warnings = yellow)
├── Dockerfile                     How to build the service image
├── docker-compose.yml             Runs the service + MySQL together
├── mvnw, mvnw.cmd                 Maven wrapper (no Maven install needed)
├── pom.xml                        Dependencies, Java version, lint/format plugins
├── README.md                      How to run and use the service
├── STRUCTURE.md                   This file
└── src/
    ├── main/
    │   ├── java/com/sportsaura/userservice/
    │   │   ├── UserServiceApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   └── response/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── mapper/
    │   │   ├── repository/
    │   │   ├── security/
    │   │   └── service/
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    └── test/
        ├── java/com/sportsaura/userservice/
        └── resources/application-test.yml
```

## Source code, package by package

Everything is under `src/main/java/com/sportsaura/userservice/`.

### `UserServiceApplication.java`

The entry point. Running its `main` method starts the whole service.

### `controller/` — the HTTP layer

Maps URLs to methods, validates the request body (`@Valid`), calls a service, returns the result.
No business logic here.

| File | URLs |
|---|---|
| `AuthController` | `POST /api/v1/auth/register`, `/login`, `/refresh`, `/logout` |
| `UserController` | `GET/PUT /api/v1/users/me`, `PATCH /api/v1/users/me/password`, `GET /api/v1/users`, `GET /api/v1/users/{id}` |

Role rules sit on the method, for example `@PreAuthorize("hasRole('ADMIN')")`.

### `service/` — the business rules

The only place that decides *what is allowed* and *what happens*.

| File | Responsibility |
|---|---|
| `AuthService` | Register (unique email, hash the password, no self-made admins), log in, refresh tokens, log out |
| `RefreshTokenService` | Creates refresh tokens (stores only a hash), uses each one once, revokes them on logout or password change, and logs the user out everywhere if a used token is replayed |
| `UserService` | Read a profile, update it, change password (which logs out every device), list users |

Methods that write to the database are marked `@Transactional`, so a failure half-way
rolls everything back.

### `repository/` — the database layer

Interfaces only. Spring Data JPA writes the SQL from the method name.

| File | Responsibility |
|---|---|
| `UserRepository` | `findByEmail`, `existsByEmail`, plus the built-in save/find/delete |
| `RefreshTokenRepository` | `findByTokenHash`, and `revokeAllForUser` (one SQL update for all of a user's tokens) |

### `entity/` — database tables as Java classes

| File | Responsibility |
|---|---|
| `User` | One row of the `users` table |
| `Role` | The allowed roles: `USER`, `TRAINER`, `ADMIN` |
| `RefreshToken` | One row of the `refresh_tokens` table: user id, token hash, expiry, when it was revoked |

`createdAt` and `updatedAt` are filled in automatically.
**If you change an entity, add a migration** (see [Migrations](#resources)).

### `dto/` — the shapes of the API

DTO means "data transfer object": a plain record describing JSON going in or out.

| Folder | Contains | Notes |
|---|---|---|
| `dto/request/` | `RegisterRequest`, `LoginRequest`, `RefreshTokenRequest`, `UpdateProfileRequest`, `ChangePasswordRequest` | Validation rules (`@NotBlank`, `@Email`, `@Size`) are written on the fields |
| `dto/response/` | `UserResponse`, `AuthResponse`, `IssuedRefreshToken` | What clients receive. `UserResponse` has no password field at all. `IssuedRefreshToken` carries a new refresh token from the service to `AuthResponse` |

### `mapper/` — entity to DTO conversion

| File | Responsibility |
|---|---|
| `UserMapper` | Converts a `User` entity into a `UserResponse` |

### `security/` — login tokens and identity

| File | Responsibility |
|---|---|
| `JwtService` | Creates the short-lived access token (JWT); checks a token and reads the user id from it |
| `JwtAuthenticationFilter` | Runs on every request: reads the `Authorization: Bearer ...` header and, if the token is valid and the user is active, marks the request as logged in |
| `UserPrincipal` | The logged-in user as controllers see it (`id`, `email`, `role`) |
| `CustomUserDetailsService` | Lets Spring Security find a user by email when checking a password |
| `RestAuthenticationEntryPoint` | The JSON `401` sent when a token is missing or invalid |
| `RestAccessDeniedHandler` | The JSON `403` sent when the role is not allowed |

### `config/` — wiring and settings

| File | Responsibility |
|---|---|
| `SecurityConfig` | Which URLs are public, password hashing (BCrypt), where the JWT filter plugs in |
| `JwtProperties` | Reads `app.jwt.*` settings and validates them at startup |
| `RefreshTokenProperties` | Reads `app.refresh-token.*` settings (how many days a refresh token lives) |
| `PersistenceConfig` | Turns on automatic `createdAt` / `updatedAt` |
| `OpenApiConfig` | Swagger UI title and the "Authorize" button |

### `exception/` — errors

| File | Responsibility |
|---|---|
| `ResourceNotFoundException` | Thrown when a record does not exist → `404` |
| `DuplicateResourceException` | Thrown when a unique value already exists → `409` |
| `InvalidRequestException` | Thrown when a business rule is broken → `400` |
| `InvalidTokenException` | Thrown when a refresh token is unknown, expired or already used → `401` |
| `GlobalExceptionHandler` | Catches every exception and returns the standard error JSON |

Services throw these exceptions; they never build HTTP responses themselves.

## Resources

`src/main/resources/`

| File | Responsibility |
|---|---|
| `application.yml` | All settings. Values come from environment variables (`${DB_HOST}` etc.), so the same file works everywhere |
| `db/migration/V1__create_users_table.sql` | Flyway migration that creates the `users` table |
| `db/migration/V2__create_refresh_tokens_table.sql` | Flyway migration that creates the `refresh_tokens` table |

Migration rules:

- Name files `V<number>__<description>.sql` (two underscores).
- Never edit a migration after it has run. Add a new one.
- Hibernate is set to `validate`: it checks tables against entities and never changes tables itself.

## Tests

`src/test/java/com/sportsaura/userservice/` mirrors the main packages.

| File | Kind | Covers |
|---|---|---|
| `AuthFlowIntegrationTest` | Integration (whole app on real MySQL) | Every endpoint, including 400/401/403/409 cases |
| `RefreshTokenFlowIntegrationTest` | Integration (real MySQL) | Refresh, one-time use, reuse detection, logout, logout-everywhere on password change |
| `FlywayMigrationIntegrationTest` | Integration (real MySQL) | Every migration applied, tables exist, unique email, delete cascade |
| `TestcontainersConfiguration` | Test setup | Starts MySQL 8.4 in Docker and connects the app to it |
| `service/AuthServiceTest` | Unit | Registration rules, refresh and logout |
| `service/RefreshTokenServiceTest` | Unit | Token hashing, expiry, one-time use, reuse detection |
| `service/UserServiceTest` | Unit | Profile update and password change |
| `security/JwtServiceTest` | Unit | Token creation, expiry, tampering |

`src/test/resources/application-test.yml` holds test-only settings (JWT secret, short token
lifetimes). The database comes from `TestcontainersConfiguration`, so integration tests need
Docker running.

## Example: what happens on "register"

1. `POST /api/v1/auth/register` arrives. `SecurityConfig` lists `/api/v1/auth/**` as public, so no token is needed.
2. `AuthController.register` receives the JSON as a `RegisterRequest`. `@Valid` checks the fields; if any fail, `GlobalExceptionHandler` returns `400` with the field errors.
3. `AuthService.register` lower-cases the email, checks it is not taken (`UserRepository.existsByEmail`), hashes the password and saves a `User`.
4. `JwtService` creates an access token and `RefreshTokenService` creates a refresh token for the new user.
5. `UserMapper` converts the `User` into a `UserResponse`, and the controller returns `201` with both tokens and the user.

## Where do I put new code?

| I want to... | Do this |
|---|---|
| Add an endpoint | Method in a `controller/` class → logic in `service/` → request/response records in `dto/` |
| Add a field to the user | New migration `V<n>__...sql` → field in `entity/User` → field in `UserResponse` and `UserMapper` (and a request DTO if clients can set it) |
| Add a new table | Migration → new class in `entity/` → new interface in `repository/` |
| Add a business rule | In the `service/` class; throw one of the `exception/` classes when it is broken |
| Add a new kind of error | New class in `exception/` + a handler method in `GlobalExceptionHandler` |
| Add a setting | Variable in `.env.example` → `application.yml` → README table. Group related ones in a `config/*Properties` record |
| Make a URL public | Add it to `PUBLIC_PATHS` in `SecurityConfig` |
| Restrict a URL to a role | `@PreAuthorize("hasRole('...')")` on the controller method |
| Add or change a lint rule | `config/checkstyle/checkstyle.xml` |

## Naming conventions

| Thing | Convention | Example |
|---|---|---|
| Package | lowercase, no underscores | `com.sportsaura.userservice` |
| Controller | `<Thing>Controller` | `UserController` |
| Service | `<Thing>Service` | `AuthService` |
| Repository | `<Entity>Repository` | `UserRepository` |
| Request DTO | `<Action>Request` | `RegisterRequest` |
| Response DTO | `<Thing>Response` | `UserResponse` |
| Exception | `<Problem>Exception` | `ResourceNotFoundException` |
| Table and column | `snake_case`, table names plural | `users.password_hash` |
| URL | `/api/v1/<plural-noun>` | `/api/v1/users/me` |
| Test class | `<ClassUnderTest>Test` | `UserServiceTest` |
