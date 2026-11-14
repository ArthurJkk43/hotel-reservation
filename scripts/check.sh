#!/usr/bin/env bash
# check.sh: verify the project before committing.
#
#   ./scripts/check.sh          full check: clean verify, then run the real .jar
#                               and smoke-test it over HTTP
#   ./scripts/check.sh quick    fast loop: clean + compile + tests only
#
# Needs: PostgreSQL running, curl, java. Run from anywhere.
# The jar runs on PORT (default 8089) so it never clashes with a dev server on 8080.

set -euo pipefail
cd "$(dirname "$0")/.."            # project root (this script lives in scripts/)

PORT="${PORT:-8089}"
BASE="http://localhost:${PORT}"
LOG="$(mktemp -t hotel-app-XXXXXX.log)"
APP_PID=""

step() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
ok()   { printf '\033[32m  ✔ %s\033[0m\n' "$*"; }
fail() {
  printf '\033[31m  ✘ %s\033[0m\n' "$*" >&2
  if [[ -n "$APP_PID" && -s "$LOG" ]]; then
    printf '\n--- last lines of the app log (%s) ---\n' "$LOG" >&2
    tail -n 30 "$LOG" >&2
  fi
  exit 1
}

cleanup() {
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# ---------------------------------------------------------------- quick mode
if [[ "${1:-}" == "quick" ]]; then
  step "Compile and run tests"
  ./mvnw -q clean test
  ok "compiled, tests passed"
  exit 0
fi

# ---------------------------------------------------------------- full mode
for tool in java curl; do
  command -v "$tool" >/dev/null || fail "'$tool' is not installed"
done

step "1/3  clean verify (compile, tests, package the jar)"
./mvnw -q clean verify
ok "build and tests passed"

JAR="$(ls -1 target/*.jar 2>/dev/null | head -n 1 || true)"
[[ -n "$JAR" ]] || fail "no .jar found in target/"
ok "built $JAR"

step "2/3  start the packaged jar on port $PORT"
if ss -ltn | grep -q ":${PORT} "; then
  fail "port $PORT is already in use (stop that process, or run: PORT=8090 $0)"
fi
java -jar "$JAR" --server.port="$PORT" >"$LOG" 2>&1 &
APP_PID=$!

for _ in $(seq 1 60); do
  grep -q "Started .* in " "$LOG" && break
  kill -0 "$APP_PID" 2>/dev/null || fail "the app exited during startup"
  sleep 1
done
grep -q "Started .* in " "$LOG" || fail "the app did not start within 60 seconds"
ok "app started"

step "3/3  smoke tests against the running jar"
http_code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
expect() {   # expect <expected status> <label> <actual status>
  if [[ "$3" == "$1" ]]; then ok "$2 -> $3"; else fail "$2: expected $1, got $3"; fi
}

JSON='Content-Type: application/json'
EMAIL="smoke+$(date +%s)@example.test"
PASS="Sm0ke-test-pass"
REGISTER="{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"firstName\":\"Smoke\",\"lastName\":\"Test\"}"
LOGIN="{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}"
BADLOGIN="{\"email\":\"$EMAIL\",\"password\":\"wrong-password\"}"

expect 201 "register a new user"       "$(http_code -X POST "$BASE/api/auth/register" -H "$JSON" -d "$REGISTER")"
expect 409 "register the same email"   "$(http_code -X POST "$BASE/api/auth/register" -H "$JSON" -d "$REGISTER")"
expect 400 "register invalid input"    "$(http_code -X POST "$BASE/api/auth/register" -H "$JSON" -d '{"email":"nope","password":"1","firstName":"","lastName":"x"}')"
expect 401 "login with wrong password" "$(http_code -X POST "$BASE/api/auth/login" -H "$JSON" -d "$BADLOGIN")"

RESPONSE="$(curl -s -X POST "$BASE/api/auth/login" -H "$JSON" -d "$LOGIN")"
TOKEN="$(sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' <<<"$RESPONSE")"
[[ -n "$TOKEN" ]] || fail "login returned no token: $RESPONSE"
ok "login returned a token"

ME="$(curl -s "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN")"
[[ "$ME" == *"$EMAIL"* && "$ME" == *"ROLE_GUEST"* ]] || fail "/me returned something unexpected: $ME"
ok "/me returned the right user and role"

expect 401 "/me without a token"       "$(http_code "$BASE/api/users/me")"
expect 401 "/me with a tampered token" "$(http_code "$BASE/api/users/me" -H "Authorization: Bearer ${TOKEN}x")"

printf '\n\033[1;32mAll checks passed. Safe to commit and push.\033[0m\n'
printf 'Smoke-test users (smoke+*@example.test) stay in the dev DB. Remove them with:\n'
printf "  DELETE FROM app_user WHERE email LIKE 'smoke+%%@example.test';\n"
