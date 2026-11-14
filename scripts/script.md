# `scripts/check.sh`: pre-commit verification script

A single script that answers the question **"is it safe to commit and push?"**
It builds the project, runs the tests, and (in full mode) starts the real packaged
`.jar` and checks it with real HTTP requests, the same way it would run in production.

```bash
./scripts/check.sh quick    # fast loop while developing
./scripts/check.sh          # full check before committing
```

Both modes stop at the first failure and exit with a non-zero code, so you can chain them:

```bash
./scripts/check.sh && git push
```

---

## Requirements

| Needed | Why |
|---|---|
| PostgreSQL running with the `hotel` database | Tests and the app connect to it |
| Java (21 or newer) | Builds and runs the app |
| `curl` | Full mode sends HTTP requests |
| `ss` (part of `iproute2`, present on most Linux systems) | Checks that the port is free |

The script must be executable once: `chmod +x scripts/check.sh`.

**Do not run it with `sudo`.** It needs no special rights, and running Maven as root creates
root-owned files in `target/` that later break `clean`.

---

## Mode 1: `./scripts/check.sh quick`

The everyday loop. Takes seconds to a minute, and does not start the app.

### What it runs

```bash
./mvnw -q clean test
```

### Step by step

| # | Maven phase | What happens |
|---|---|---|
| 1 | `clean` | Deletes the `target/` folder so nothing stale is left over |
| 2 | `compile` | Compiles `src/main/java`. Compiler errors stop the run here |
| 3 | `test-compile` | Compiles `src/test/java` |
| 4 | `test` | Runs all tests (`*Test`, `*Tests`), including the Spring Boot tests that use the real database |

`-q` (quiet) hides Maven's normal output. You only see output when something fails.

### Result

- **Success:** the last line is `✔ compiled, tests passed`.
- **Failure:** Maven prints the compiler error or the failed test, and the script exits with an error.

---

## Mode 2: `./scripts/check.sh` (full check)

Run this before every commit. It proves that the project **builds, passes its tests, and
works when started from the packaged `.jar`**.

### Step 1/3: build and test

```bash
./mvnw -q clean verify
```

`verify` runs every earlier phase in order, so this one command replaces running
`compile`, `test` and `package` separately:

```
clean → compile → test-compile → test → package (builds the .jar) → verify
```

| Phase | What happens |
|---|---|
| `clean` | Deletes `target/` |
| `compile`, `test-compile` | Compiles main and test code |
| `test` | Runs all tests |
| `package` | Builds the executable `.jar` in `target/` |
| `verify` | Runs any integration checks configured on the build (none yet) |

Right after the build, the script finds the jar:

```bash
ls -1 target/*.jar | head -n 1
```

### Step 2/3: start the packaged jar

1. **Check the port is free** (default **8089**, so it never clashes with a dev server on 8080):
   ```bash
   ss -ltn | grep -q ":8089 "
   ```
   If something is listening, the script stops and tells you. Use another port with
   `PORT=8090 ./scripts/check.sh`.
2. **Start the app in the background**, writing its log to a temporary file:
   ```bash
   java -jar target/hotel-reservation-0.0.1-SNAPSHOT.jar --server.port=8089
   ```
3. **Wait up to 60 seconds** for the log line `Started ... in ...`. If the app exits early or
   never starts, the script prints the last 30 lines of the app log and fails.

### Step 3/3: smoke tests

The script sends real HTTP requests to `http://localhost:8089` and checks each status code.
It registers a throwaway user named `smoke+<timestamp>@example.test`.

| # | Request | Expected | What it proves |
|---|---|---|---|
| 1 | `POST /api/auth/register` (new user) | `201` | Registration works |
| 2 | `POST /api/auth/register` (same email) | `409` | Duplicate emails are refused |
| 3 | `POST /api/auth/register` (invalid body) | `400` | Validation works |
| 4 | `POST /api/auth/login` (wrong password) | `401` | Bad credentials are refused |
| 5 | `POST /api/auth/login` (correct password) | token returned | Login issues a JWT |
| 6 | `GET /api/users/me` with the token | body contains the email and `ROLE_GUEST` | The token is verified and roles are mapped |
| 7 | `GET /api/users/me` without a token | `401` | Protected endpoints require login |
| 8 | `GET /api/users/me` with a tampered token | `401` | Forged tokens are rejected |

### Cleanup (always runs)

When the script ends, whether it passes, fails or you press Ctrl+C, it stops the background app
(`kill` on its process id). Nothing is left running on the port.

### Result

- **Success:** `All checks passed. Safe to commit and push.`
- **Failure:** a red `✘` line says which check failed (for example `expected 201, got 500`),
  followed by the last lines of the app log.

---

## What the script leaves behind

Each full run adds one test user (`smoke+...@example.test`) to your **development** database.
Remove them whenever you like:

```sql
DELETE FROM app_user WHERE email LIKE 'smoke+%@example.test';
```

Their rows in `user_role` are removed automatically.

---

## Other useful commands (not part of the script)

| Command | What it does |
|---|---|
| `./mvnw spring-boot:run` | Runs the app directly from source, for development, on port 8080 |
| `./mvnw clean package -DskipTests` | Builds the jar without running tests |
| `java -jar target/hotel-reservation-0.0.1-SNAPSHOT.jar` | Runs the packaged jar by hand |
| `ss -ltnp \| grep 8080` | Shows which process is using port 8080 |

---

## Troubleshooting

| Message | Cause and fix |
|---|---|
| `Permission denied` when running the script | Run `chmod +x scripts/check.sh` |
| `Failed to clean project: Failed to delete ... target/...` | Files in `target/` are owned by root, from an earlier `sudo` run. Fix with `sudo chown -R "$USER":"$USER" .` and rerun without `sudo` |
| `port 8089 is already in use` | Another process uses it. Stop it, or run `PORT=8090 ./scripts/check.sh` |
| `the app exited during startup` | Read the log lines printed below the message. Usually the database is not running, or a bean or configuration is broken |
| `connection refused` or `password authentication failed` in the log | PostgreSQL is not running, or `DB_PASSWORD` does not match your database user |
| `expected 201, got 500` and similar | A real bug. The app log printed below shows the exception |