#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
prepare_database
(cd frontend && npm ci --no-audit --no-fund)
(cd backend && run_maven package -DskipTests)
export SPRING_PROFILES_ACTIVE=dev
java -jar backend/target/simulator-0.0.1-SNAPSHOT.jar > .local/backend.log 2>&1 &
backend_pid=$!
echo "$backend_pid" > .local/backend.pid
cleanup() {
  kill "$backend_pid" "${frontend_pid:-}" 2>/dev/null || true
}
trap cleanup EXIT INT TERM
ready=false
for attempt in $(seq 1 90); do
  if curl -fsS --max-time 2 http://127.0.0.1:8080/api/health > .local/health.json 2>/dev/null; then
    ready=true; break
  fi
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    tail -n 30 .local/backend.log >&2; exit 1
  fi
  sleep 1
done
if ! "$ready"; then echo 'Backend did not become healthy; see .local/backend.log' >&2; exit 1; fi
(cd frontend && exec node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5173 --strictPort) > .local/frontend.log 2>&1 &
frontend_pid=$!
echo "$frontend_pid" > .local/frontend.pid
echo 'Development services started: frontend port 5173, backend port 8080. Logs are in .local/. Press Ctrl-C to stop both; MySQL data is retained.'
wait -n "$backend_pid" "$frontend_pid"
