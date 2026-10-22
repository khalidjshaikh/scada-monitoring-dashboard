#!/usr/bin/env bash
#
# Meridian SCADA — stop launcher.
#
#   ./stop.sh          stop every session started by ./run.sh
#   ./run.sh stop      same thing
#
# Sessions are recorded by run.sh in .run/. If no session record exists, the
# script scans for Java/Vite processes that belong to this checkout and verifies
# them before terminating anything.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
FAILURES=0

info() { printf '\033[36m[meridian]\033[0m %s\n' "$*"; }
warn() { printf '\033[33m[meridian]\033[0m %s\n' "$*"; }
fail() { printf '\033[31m[meridian]\033[0m %s\n' "$*" >&2; exit 1; }

usage() {
  sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'
}

if [ "${1:-}" = "help" ] || [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
  usage
  exit 0
fi

is_numeric() {
  case "${1:-}" in
    '' | *[!0-9]*) return 1 ;;
    *) return 0 ;;
  esac
}

pid_alive() {
  is_numeric "${1:-}" && kill -0 "$1" 2>/dev/null
}

command_of() {
  ps -ww -p "$1" -o command= 2>/dev/null || true
}

cwd_of() {
  command -v lsof >/dev/null 2>&1 || return 0
  lsof -a -p "$1" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | head -n 1
}

is_backend_pid() {
  local pid="$1"
  local command
  command="$(command_of "$pid")"
  [ -n "$command" ] || return 1
  [[ "$command" == *"scada-monitoring-dashboard"* ]] || return 1
  [[ "$command" == *"java"* ]] || return 1
  [[ "$command" == *"$BACKEND_DIR"* ]] && return 0
  [ "$(cwd_of "$pid")" = "$BACKEND_DIR" ]
}

is_frontend_pid() {
  local pid="$1"
  local port="${2:-}"
  local command
  command="$(command_of "$pid")"
  [ -n "$command" ] || return 1
  [[ "$command" == *"vite"* ]] || return 1
  if [ -n "$port" ]; then
    [[ "$command" == *"--port $port"* ]] || return 1
  fi
  local cwd
  cwd="$(cwd_of "$pid")"
  if [ -n "$cwd" ]; then
    [ "$cwd" = "$FRONTEND_DIR" ] || return 1
  fi
  return 0
}

is_launcher_pid() {
  local pid="$1"
  local command
  command="$(command_of "$pid")"
  [ -n "$command" ] || return 1
  [[ "$command" == *"run.sh"* ]] || return 1
  local cwd
  cwd="$(cwd_of "$pid")"
  [ -z "$cwd" ] || [ "$cwd" = "$ROOT_DIR" ]
}

stop_pid() {
  local pid="$1"
  local label="$2"
  local attempts=40

  if ! pid_alive "$pid"; then
    return 0
  fi

  kill -TERM "$pid" 2>/dev/null || true
  local i
  for ((i = 0; i < attempts; i++)); do
    if ! pid_alive "$pid"; then
      info "$label stopped."
      return 0
    fi
    sleep 0.1
  done

  warn "$label did not exit after 4s — forcing."
  kill -KILL "$pid" 2>/dev/null || true
  sleep 0.25
  if pid_alive "$pid"; then
    warn "$label could not be stopped."
    return 1
  fi
  info "$label stopped."
  return 0
}

stop_session() {
  local file="$1"
  local mode="" launcher="" backend="" backend_port="" frontend="" frontend_port=""

  while IFS='=' read -r key value; do
    case "$key" in
      MODE) mode="$value" ;;
      LAUNCHER_PID) launcher="$value" ;;
      BACKEND_PID) backend="$value" ;;
      BACKEND_PORT) backend_port="$value" ;;
      FRONTEND_PID) frontend="$value" ;;
      FRONTEND_PORT) frontend_port="$value" ;;
    esac
  done < "$file"

  info "Stopping session${launcher:+ $launcher} (${mode:-dev}) — console :${frontend_port:-?}, API :${backend_port:-?}"

  if pid_alive "$frontend"; then
    if is_frontend_pid "$frontend" "$frontend_port"; then
      stop_pid "$frontend" "React console (pid $frontend)" || FAILURES=$((FAILURES + 1))
    else
      warn "Skipping pid $frontend — it no longer belongs to the Meridian console."
    fi
  fi

  if pid_alive "$backend"; then
    if is_backend_pid "$backend"; then
      stop_pid "$backend" "SCADA service (pid $backend)" || FAILURES=$((FAILURES + 1))
    else
      warn "Skipping pid $backend — it no longer belongs to the Meridian service."
    fi
  fi

  if pid_alive "$launcher" && is_launcher_pid "$launcher"; then
    kill -TERM "$launcher" 2>/dev/null || true
    local i
    for ((i = 0; i < 20; i++)); do
      pid_alive "$launcher" || break
      sleep 0.1
    done
    if pid_alive "$launcher"; then
      kill -KILL "$launcher" 2>/dev/null || true
    fi
  fi

  rm -f "$file"
}

scan_unmanaged() {
  local found=0
  local pids pid

  info "No tracked session found — scanning for Meridian processes…"

  if command -v lsof >/dev/null 2>&1; then
    pids="$(pgrep -f 'vite' 2>/dev/null || true)"
    for pid in $pids; do
      if is_frontend_pid "$pid"; then
        info "Found untracked console process (pid $pid)."
        stop_pid "$pid" "React console (pid $pid)" || FAILURES=$((FAILURES + 1))
        found=1
      fi
    done
  else
    warn "lsof is unavailable; skipping untracked Vite process scan."
  fi

  pids="$(pgrep -f 'scada-monitoring-dashboard.*\.jar' 2>/dev/null || true)"
  for pid in $pids; do
    if is_backend_pid "$pid"; then
      info "Found untracked SCADA process (pid $pid)."
      stop_pid "$pid" "SCADA service (pid $pid)" || FAILURES=$((FAILURES + 1))
      found=1
    fi
  done

  if [ "$found" -eq 0 ]; then
    info "No Meridian SCADA processes are running."
  fi
}

main() {
  local sessions=("$RUN_DIR"/*.env)
  local stopped=0

  if [ -f "${sessions[0]:-}" ]; then
    for file in "${sessions[@]}"; do
      [ -f "$file" ] || continue
      stop_session "$file"
      stopped=1
    done
  else
    scan_unmanaged
  fi

  if [ "$stopped" -eq 1 ]; then
    rmdir "$RUN_DIR" 2>/dev/null || true
  fi

  if [ "$FAILURES" -gt 0 ]; then
    fail "$FAILURES process group(s) could not be stopped. Check the process list."
  fi
  exit 0
}

main
