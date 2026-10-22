#!/usr/bin/env bash
#
# Meridian SCADA — development / production launcher.
#
#   ./run.sh            dev console  (Vite + Spring Boot, live reload for UI)
#   ./run.sh prod       build everything, serve UI + API from one jar
#   ./run.sh build      build frontend + backend without starting
#   ./run.sh stop       stop sessions started by this launcher
#   ./run.sh help
#
# Ports auto-select from 8082 (API) and 5173 (console) when free.
# Both services bind dual-stack IPv6 (::) by default. To force IPv4 only:
#   BIND_HOST=0.0.0.0 ./run.sh
# To bind only IPv6 loopback:
#   BIND_HOST=::1 ./run.sh
# Pin ports:
#   BACKEND_PORT=8090 FRONTEND_PORT=5180 ./run.sh
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
RUN_DIR="$ROOT_DIR/.run"
SESSION_FILE="$RUN_DIR/session-$$.env"
BIND_HOST="${BIND_HOST:-::}"
BACKEND_PORT="${BACKEND_PORT:-auto}"
FRONTEND_PORT="${FRONTEND_PORT:-auto}"
BACKEND_PROBE_URL=""
BACKEND_TARGET=""
MODE="${1:-dev}"
case "$MODE" in
  --dev) MODE="dev" ;;
  --prod) MODE="prod" ;;
  --build) MODE="build" ;;
esac

BACKEND_PID=""
FRONTEND_PID=""
BACKEND_JAR=""

info()  { printf '\033[36m[meridian]\033[0m %s\n' "$*"; }
warn()  { printf '\033[33m[meridian]\033[0m %s\n' "$*"; }
fail()  { printf '\033[31m[meridian]\033[0m %s\n' "$*" >&2; exit 1; }

need() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

port_busy() {
  command -v lsof >/dev/null 2>&1 &&
    lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

port_owner() {
  command -v lsof >/dev/null 2>&1 || return 0
  lsof -nP -iTCP:"$1" -sTCP:LISTEN 2>/dev/null | awk 'NR==2 {print $1 " (pid " $2 ")"}'
}

find_free_port() {
  local port="$1"
  local end="$2"
  while [ "$port" -le "$end" ]; do
    if ! port_busy "$port"; then
      printf '%s' "$port"
      return 0
    fi
    port=$((port + 1))
  done
  return 1
}

resolve_port() {
  local requested="$1"
  local start="$2"
  local end="$3"
  local label="$4"
  if [ "$requested" = "auto" ]; then
    local found
    found="$(find_free_port "$start" "$end")" ||
      fail "No free $label port in range ${start}-${end}"
    printf '%s' "$found"
    return 0
  fi
  if port_busy "$requested"; then
    fail "$label port $requested is in use by $(port_owner "$requested")"
  fi
  printf '%s' "$requested"
}

# Turn a bind address into a URL-safe host. Wildcard binds are probed over
# loopback so health checks never target 0.0.0.0 / :: directly.
endpoint_host() {
  local host="$1"
  case "$host" in
    "::" | "[::]" | "::1" | "[::1]") printf '[::1]' ;;
    0.0.0.0) printf '127.0.0.1' ;;
    \[*\]) printf '%s' "$host" ;;
    *:*) printf '[%s]' "$host" ;;
    *) printf '%s' "$host" ;;
  esac
}

display_host() {
  local host="$1"
  case "$host" in
    \[*\]) printf '%s' "$host" ;;
    *:*) printf '[%s]' "$host" ;;
    *) printf '%s' "$host" ;;
  esac
}

# Strip IPv6 brackets for Java/Vite bind arguments.
bind_address() {
  local host="$1"
  case "$host" in
    \[*\]) host="${host#[}"; host="${host%]}" ;;
  esac
  printf '%s' "$host"
}

is_dual_stack() {
  case "$BIND_HOST" in
    "::" | "[::]") return 0 ;;
    *) return 1 ;;
  esac
}

install_frontend() {
  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    info "Installing console dependencies…"
    (cd "$FRONTEND_DIR" && npm install --no-audit --no-fund)
  fi
}

build_frontend() {
  need npm
  install_frontend
  info "Building React console…"
  (cd "$FRONTEND_DIR" && npm run build)
}

backend_jar() {
  find "$BACKEND_DIR/target" -maxdepth 1 -type f -name '*.jar' \
    ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*.original' 2>/dev/null |
    head -n 1
}

backend_source_hash() {
  (
    cd "$BACKEND_DIR"
    find pom.xml src/main/java src/main/resources \
      -type f ! -path 'src/main/resources/static/*' -print0 |
      xargs -0 shasum | sort | shasum | awk '{print $1}'
  )
}

build_backend() {
  need java
  local force="${1:-false}"
  local existing hash stamp
  existing="$(backend_jar)"
  hash="$(backend_source_hash)"
  stamp="$BACKEND_DIR/target/.backend-source-hash"
  if [ "$force" != "true" ] && [ -n "$existing" ] && [ -f "$stamp" ] &&
    [ "$(cat "$stamp")" = "$hash" ]; then
    BACKEND_JAR="$existing"
    info "Using existing service build…"
    return 0
  fi
  info "Compiling Spring Boot service…"
  (cd "$BACKEND_DIR" && ./mvnw -q -DskipTests package)
  BACKEND_JAR="$(backend_jar)"
  [ -n "$BACKEND_JAR" ] || fail "Backend jar was not produced"
  printf '%s\n' "$hash" > "$stamp"
}

record_session() {
  mkdir -p "$RUN_DIR"
  {
    printf 'MODE=%s\n' "$MODE"
    printf 'LAUNCHER_PID=%s\n' "$$"
    printf 'BACKEND_PID=%s\n' "${BACKEND_PID:-}"
    printf 'BACKEND_PORT=%s\n' "${BACKEND_PORT:-auto}"
    printf 'FRONTEND_PID=%s\n' "${FRONTEND_PID:-}"
    printf 'FRONTEND_PORT=%s\n' "${FRONTEND_PORT:-auto}"
    printf 'STARTED_AT=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  } > "$SESSION_FILE"
  chmod 600 "$SESSION_FILE" 2>/dev/null || true
}

cleanup() {
  local code=$?
  trap - EXIT INT TERM
  if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    info "Stopping console…"
    kill "$FRONTEND_PID" 2>/dev/null || true
    wait "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    info "Stopping SCADA service…"
    kill "$BACKEND_PID" 2>/dev/null || true
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
  rm -f "$SESSION_FILE"
  rmdir "$RUN_DIR" 2>/dev/null || true
  exit "$code"
}

check_versions() {
  need java
  need node
  need npm
  local java_major node_major
  java_major="$(java -version 2>&1 | awk -F '"' '/version/ {split($2, v, "."); print v[1]}')"
  node_major="$(node -p 'process.versions.node.split(".")[0]')"
  if [ -n "$java_major" ] && [ "$java_major" -lt 27 ] 2>/dev/null; then
    warn "Java $java_major detected — this project targets Java 27."
  fi
  if [ -n "$node_major" ] && [ "$node_major" -lt 22 ] 2>/dev/null; then
    warn "Node $node_major detected — Vite 8 needs Node 22 or newer."
  fi
}

wait_for_backend() {
  local attempts=90
  local probe="${BACKEND_PROBE_URL:-http://127.0.0.1:${BACKEND_PORT}}"
  for ((i = 0; i < attempts; i++)); do
    if curl -fsS "${probe}/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    if [ -n "$BACKEND_PID" ] && ! kill -0 "$BACKEND_PID" 2>/dev/null; then
      return 1
    fi
    sleep 1
  done
  return 1
}

run_dev() {
  check_versions
  install_frontend
  need curl

  local jar address endpoint
  BACKEND_PORT="$(resolve_port "$BACKEND_PORT" 8082 8099 "Backend")"
  FRONTEND_PORT="$(resolve_port "$FRONTEND_PORT" 5173 5199 "Console")"
  address="$(bind_address "$BIND_HOST")"
  endpoint="$(endpoint_host "$BIND_HOST")"
  BACKEND_PROBE_URL="http://${endpoint}:${BACKEND_PORT}"
  BACKEND_TARGET="${VITE_BACKEND_TARGET:-${BACKEND_PROBE_URL}}"
  build_backend
  jar="$BACKEND_JAR"

  info "Starting SCADA API on $(display_host "$BIND_HOST"):${BACKEND_PORT}…"
  (cd "$BACKEND_DIR" && exec java ${JAVA_OPTS:-} -jar "$jar" \
    --server.port="$BACKEND_PORT" --server.address="$address") &
  BACKEND_PID=$!
  record_session
  trap cleanup EXIT INT TERM

  if ! wait_for_backend; then
    fail "Backend did not become healthy at ${BACKEND_PROBE_URL}. Check the log above."
  fi

  info "Starting React console on $(display_host "$BIND_HOST"):${FRONTEND_PORT}…"
  (
    cd "$FRONTEND_DIR"
    VITE_BACKEND_TARGET="$BACKEND_TARGET" \
      exec ./node_modules/.bin/vite --port "$FRONTEND_PORT" --strictPort --host "$address"
  ) &
  FRONTEND_PID=$!
  record_session

  printf '\n'
  info "Console  →  http://${endpoint}:${FRONTEND_PORT}"
  info "API      →  http://${endpoint}:${BACKEND_PORT}/api/dashboard"
  info "Health   →  http://${endpoint}:${BACKEND_PORT}/actuator/health"
  info "H2       →  http://${endpoint}:${BACKEND_PORT}/h2-console"
  if is_dual_stack; then
    info "IPv4     →  http://127.0.0.1:${FRONTEND_PORT}"
  fi
  printf '\n'

  while kill -0 "$BACKEND_PID" 2>/dev/null && kill -0 "$FRONTEND_PID" 2>/dev/null; do
    sleep 2
  done
}

run_prod() {
  check_versions
  local address endpoint
  BACKEND_PORT="$(resolve_port "$BACKEND_PORT" 8082 8099 "Backend")"
  address="$(bind_address "$BIND_HOST")"
  endpoint="$(endpoint_host "$BIND_HOST")"
  BACKEND_PROBE_URL="http://${endpoint}:${BACKEND_PORT}"
  build_frontend
  local jar
  build_backend true
  jar="$BACKEND_JAR"

  info "Serving Meridian console and API on $(display_host "$BIND_HOST"):${BACKEND_PORT}…"
  (cd "$BACKEND_DIR" && exec java ${JAVA_OPTS:-} -jar "$jar" \
    --server.port="$BACKEND_PORT" --server.address="$address") &
  BACKEND_PID=$!
  record_session
  trap cleanup EXIT INT TERM

  if ! wait_for_backend; then
    fail "Backend did not become healthy at ${BACKEND_PROBE_URL}. Check the log above."
  fi
  printf '\n'
  info "Console  →  http://${endpoint}:${BACKEND_PORT}"
  info "API      →  http://${endpoint}:${BACKEND_PORT}/api/dashboard"
  if is_dual_stack; then
    info "IPv4     →  http://127.0.0.1:${BACKEND_PORT}"
  fi
  printf '\n'
  wait "$BACKEND_PID"
}

main() {
  case "$MODE" in
    dev)
      run_dev
      ;;
    prod)
      run_prod
      ;;
    stop)
      if [ -x "$ROOT_DIR/stop.sh" ]; then
        exec "$ROOT_DIR/stop.sh"
      fi
      fail "stop.sh is missing or not executable"
      ;;
    build)
      check_versions
      build_frontend
      build_backend true
      info "Build complete. Jar: $(backend_jar)"
      ;;
    help | --help | -h)
      sed -n '2,/^set -euo pipefail$/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'
      ;;
    *)
      fail "Unknown mode '$MODE'. Use: dev | prod | build | help"
      ;;
  esac
}

main
