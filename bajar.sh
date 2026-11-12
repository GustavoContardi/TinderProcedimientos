#!/usr/bin/env bash
# Baja todo lo que levantó ./levantar.sh
# Uso: ./bajar.sh          -> baja frontend, backend y PostgreSQL (los datos quedan)
#      ./bajar.sh --reset  -> además borra la base (el próximo ./levantar.sh recarga el seed)
set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
RUN="$ROOT/.run"
cd "$ROOT"

info() { printf '\033[36m▶ %s\033[0m\n' "$*"; }

parar() {  # nombre
  local nombre=$1 pidfile="$RUN/$1.pid" pid
  if [[ ! -f "$pidfile" ]]; then info "$nombre: no estaba corriendo"; return; fi
  pid=$(cat "$pidfile")
  if kill -0 "$pid" 2>/dev/null; then
    info "Bajando $nombre (pid $pid)"
    # Matamos el grupo entero (mvnw -> java, npx -> ng serve -> node)
    kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null
    for _ in {1..15}; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
    kill -KILL -- "-$pid" 2>/dev/null || true
  else
    info "$nombre: no estaba corriendo"
  fi
  rm -f "$pidfile"
}

parar frontend
parar backend

if [[ "${1:-}" == "--reset" ]]; then
  info "Bajando PostgreSQL y borrando la base"
  docker compose down -v
else
  info "Bajando PostgreSQL (los datos se conservan)"
  docker compose down
fi

printf '\033[32m✓ Todo abajo\033[0m\n'
