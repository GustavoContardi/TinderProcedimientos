#!/usr/bin/env bash
# Levanta todo: PostgreSQL (Docker) + backend (Spring Boot) + frontend (Angular).
# Uso: ./levantar.sh           -> levanta todo
#      ./levantar.sh --reset   -> borra la base y la vuelve a cargar con el seed
# Logs en .run/backend.log y .run/frontend.log. Para bajar todo: ./bajar.sh
set -euo pipefail
set -m  # cada proceso en background queda en su propio grupo (así ./bajar.sh mata el árbol entero)

ROOT="$(cd "$(dirname "$0")" && pwd)"
RUN="$ROOT/.run"
mkdir -p "$RUN"
cd "$ROOT"

verde() { printf '\033[32m%s\033[0m\n' "$*"; }
info()  { printf '\033[36m▶ %s\033[0m\n' "$*"; }
error() { printf '\033[31m✗ %s\033[0m\n' "$*" >&2; }

corriendo() { [[ -f "$RUN/$1.pid" ]] && kill -0 "$(cat "$RUN/$1.pid")" 2>/dev/null; }
puerto_ocupado() { lsof -iTCP:"$1" -sTCP:LISTEN -t >/dev/null 2>&1; }

esperar_puerto() {  # nombre puerto timeout_seg
  local nombre=$1 puerto=$2 timeout=$3 t=0
  printf '  esperando %s en :%s ' "$nombre" "$puerto"
  until puerto_ocupado "$puerto"; do
    if ! corriendo "$nombre"; then
      echo; error "$nombre se cayó. Últimas líneas de .run/$nombre.log:"
      tail -n 30 "$RUN/$nombre.log" >&2; exit 1
    fi
    if (( t >= timeout )); then
      echo; error "$nombre no levantó en ${timeout}s. Mirá .run/$nombre.log"; exit 1
    fi
    printf '.'; sleep 2; t=$((t + 2))
  done
  echo " ok"
}

# --- JDK 21 (el default de la máquina puede ser otro) ---
buscar_java21() {
  local candidatos=(
    "${JAVA_HOME:-}"
    "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    "$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  )
  for j in "${candidatos[@]}"; do
    [[ -n "$j" && -x "$j/bin/java" ]] || continue
    "$j/bin/java" -version 2>&1 | head -1 | grep -q '"21' && { echo "$j"; return; }
  done
  return 1
}

# --- 1. Base de datos ---
if [[ "${1:-}" == "--reset" ]]; then
  info "Reseteando la base (docker compose down -v)"
  docker compose down -v
fi
info "Levantando PostgreSQL"
docker compose up -d --wait

# --- 2. Backend ---
if corriendo backend; then
  info "Backend ya estaba corriendo (pid $(cat "$RUN/backend.pid"))"
elif puerto_ocupado 8080; then
  error "El puerto 8080 está ocupado por otro proceso:"; lsof -iTCP:8080 -sTCP:LISTEN >&2; exit 1
else
  JAVA21="$(buscar_java21)" || { error "No encontré un JDK 21. Instalalo: brew install openjdk@21"; exit 1; }
  info "Levantando backend (JDK: $JAVA21)"
  ( cd backend && JAVA_HOME="$JAVA21" exec nohup ./mvnw -q spring-boot:run ) < /dev/null > "$RUN/backend.log" 2>&1 &
  echo $! > "$RUN/backend.pid"
fi

# --- 3. Frontend ---
if corriendo frontend; then
  info "Frontend ya estaba corriendo (pid $(cat "$RUN/frontend.pid"))"
elif puerto_ocupado 4200; then
  error "El puerto 4200 está ocupado por otro proceso:"; lsof -iTCP:4200 -sTCP:LISTEN >&2; exit 1
else
  if [[ ! -d frontend/node_modules ]]; then
    info "Instalando dependencias del frontend (npm ci, solo la primera vez)"
    ( cd frontend && npm ci )
  fi
  info "Levantando frontend"
  ( cd frontend && NG_CLI_ANALYTICS=false exec nohup npx ng serve --no-open ) < /dev/null > "$RUN/frontend.log" 2>&1 &
  echo $! > "$RUN/frontend.pid"
fi

esperar_puerto backend 8080 240
esperar_puerto frontend 4200 180

echo
verde "✓ Todo arriba"
echo "  App:     http://localhost:4200"
echo "  API:     http://localhost:8080/api"
echo "  Swagger: http://localhost:8080/swagger-ui.html"
echo "  Logs:    tail -f .run/backend.log .run/frontend.log"
echo "  Bajar:   ./bajar.sh"
