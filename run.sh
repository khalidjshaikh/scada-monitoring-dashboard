#!/usr/bin/env bash
#
# Meridian SCADA — development / production launcher.
#
#   ./run.sh            dev console  (Vite + Spring Boot, live reload for UI)
#   ./run.sh prod       build everything, serve UI + API from one jar
#   ./run.sh build      build frontend + backend without starting
#   ./run.sh help
#
# Ports can be overridden:
#   BACKEND_PORT=8090 FRONTEND_PORT=5174 ./run.sh
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_PORT="${BACKEND_PORT:-8082}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
MODE="${1:-dev}"

BACKEND_PID=""
FRONTEND_PID=""

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

build_backend() {
  need java
  info "Compiling Spring Boot service…"
  (cd "$BACKEND_DIR" && ./mvnw -q -DskipTests package)
  local jar
  jar="$(backend_jar)"
  [ -n "$jar" ] || fail "Backend jar was not produced"
  printf '%s\n' "$jar"
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
  for ((i = 0; i < attempts; i++)); do
    if curl -fsS "http://127.0.0.1:${BACKEND_PORT}/actuator/health" >/dev/null 2>&1; then
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

  if port_busy "$BACKEND_PORT"; then
    fail "Port $BACKEND_PORT is in use by $(port_owner "$BACKEND_PORT"). Set BACKEND_PORT to another port."
  fi
  if port_busy "$FRONTEND_PORT"; then
    fail "Port $FRONTEND_PORT is in use by $(port_owner "$FRONTEND_PORT"). Set FRONTEND_PORT to another port."
  fi

  local jar
  jar="$(build_backend)"

  info "Starting SCADA API on :$BACKEND_PORT…"
  (cd "$BACKEND_DIR" && exec java ${JAVA_OPTS:-} -jar "$jar" --server.port="$BACKEND_PORT") &
  BACKEND_PID=$!
  trap cleanup EXIT INT TERM

  if ! wait_for_backend; then
    fail "Backend did not become healthy. Check the log above."
  fi

  info "Starting React console on :$FRONTEND_PORT…"
  (
    cd "$FRONTEND_DIR"
    VITE_BACKEND_TARGET="http://127.0.0.1:${BACKEND_PORT}" \
      exec ./node_modules/.bin/vite --port "$FRONTEND_PORT" --strictPort --host 127.0.0.1
  ) &
  FRONTEND_PID=$!

  printf '\n'
  info "Console  →  http://localhost:${FRONTEND_PORT}"
  info "API      →  http://localhost:${BACKEND_PORT}/api/dashboard"
  info "Health   →  http://localhost:${BACKEND_PORT}/actuator/health"
  info "H2       →  http://localhost:${BACKEND_PORT}/h2-console"
  printf '\n'

  while kill -0 "$BACKEND_PID" 2>/dev/null && kill -0 "$FRONTEND_PID" 2>/dev/null; do
    sleep 2
  done
}

run_prod() {
  check_versions
  if port_busy "$BACKEND_PORT"; then
    fail "Port $BACKEND_PORT is in use by $(port_owner "$BACKEND_PORT"). Set BACKEND_PORT to another port."
  fi
  build_frontend
  local jar
  jar="$(build_backend)"

  info "Serving Meridian console and API on :$BACKEND_PORT…"
  (cd "$BACKEND_DIR" && exec java ${JAVA_OPTS:-} -jar "$jar" --server.port="$BACKEND_PORT") &
  BACKEND_PID=$!
  trap cleanup EXIT INT TERM

  if ! wait_for_backend; then
    fail "Backend did not become healthy. Check the log above."
  fi
  printf '\n'
  info "Console  →  http://localhost:${BACKEND_PORT}"
  info "API      →  http://localhost:${BACKEND_PORT}/api/dashboard"
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
    build)
      check_versions
      build_frontend
      build_backend >/dev/null
      info "Build complete. Jar: $(backend_jar)"
      ;;
    help | --help | -h)
      sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
      ;;
    *)
      fail "Unknown mode '$MODE'. Use: dev | prod | build | help"
      ;;
  esac
}

main
