#!/usr/bin/env bash
set -euo pipefail

# Starts only services that are not already healthy. A process occupying a
# required port is never killed: it could be user-managed and must be resolved
# explicitly instead of being replaced.

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$PROJECT_ROOT/scripts/logs"
TUNNEL_STATE_FILE="$LOG_DIR/last-deployed-tunnel-url"
mkdir -p "$LOG_DIR"

FRONTEND_URL="https://ai-meeting-knowledge-platform.vercel.app"
AI_URL="http://127.0.0.1:8000"
BACKEND_URL="http://127.0.0.1:8080"

echo "=================================================================="
echo "   AI MEETING KNOWLEDGE PLATFORM - PRESENTATION STARTUP SYSTEM    "
echo "=================================================================="
echo "Project Root: $PROJECT_ROOT"
echo "Target Frontend: $FRONTEND_URL"
echo "------------------------------------------------------------------"

healthy_ai() { curl -fsS --max-time 5 "$AI_URL/health" 2>/dev/null | grep -q '"status":"ok"'; }
healthy_backend() { curl -fsS --max-time 5 "$BACKEND_URL/health" 2>/dev/null | grep -q '"status":"UP"'; }
healthy_tunnel() { [ -n "$1" ] && curl -fsS --max-time 10 "$1/health" 2>/dev/null | grep -q '"status":"UP"'; }
port_pids() { lsof -nP -t -iTCP:"$1" -sTCP:LISTEN 2>/dev/null | sort -u || true; }

wait_for() {
    local description="$1" attempts="$2" check="$3" i
    for ((i = 1; i <= attempts; i++)); do
        "$check" && return 0
        sleep 1
    done
    echo "ERROR: $description did not pass its health check."
    return 1
}

start_ai() {
    local pids
    pids="$(port_pids 8000)"
    if [ -n "$pids" ]; then
        echo "ERROR: Port 8000 is occupied, but it is not the expected healthy FastAPI service (PID(s): $pids)."
        echo "Refusing to kill or replace it. Resolve the process and run this script again."
        exit 1
    fi
    echo "  -> Starting FastAPI AI service..."
    (cd "$PROJECT_ROOT/ai-service" && nohup .venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 > "$LOG_DIR/ai-service.log" 2>&1 &)
    wait_for "FastAPI AI service on port 8000" 45 healthy_ai || { tail -n 40 "$LOG_DIR/ai-service.log" 2>/dev/null || true; exit 1; }
}

start_backend() {
    local pids
    pids="$(port_pids 8080)"
    if [ -n "$pids" ]; then
        echo "ERROR: Port 8080 is occupied, but it is not the expected healthy Spring Boot backend (PID(s): $pids)."
        echo "Refusing to kill or replace it. Resolve the process and run this script again."
        exit 1
    fi
    echo "  -> Starting Spring Boot backend..."
    (cd "$PROJECT_ROOT/backend" && nohup env FRONTEND_ORIGIN="$FRONTEND_URL" AI_SERVICE_URL="$AI_URL" mvn spring-boot:run > "$LOG_DIR/backend.log" 2>&1 &)
    wait_for "Spring Boot backend on port 8080" 60 healthy_backend || { tail -n 60 "$LOG_DIR/backend.log" 2>/dev/null || true; exit 1; }
}

# 1. Dependency Checks
echo "[1/6] Checking System Dependencies..."

if ! command -v cloudflared &> /dev/null; then
    echo "ERROR: 'cloudflared' command not found. Install via: brew install cloudflared"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "ERROR: 'mvn' (Maven) command not found."
    exit 1
fi

if ! command -v vercel &> /dev/null; then
    echo "ERROR: 'vercel' CLI command not found. Install via: npm install -g vercel"
    exit 1
fi

if ! command -v psql &> /dev/null; then
    echo "WARN: 'psql' CLI not found, checking database connection via backend."
else
    if ! psql -U vyankateshtalokar -d ai_meeting_platform -c "SELECT 1;" &> /dev/null; then
        echo "ERROR: Unable to connect to PostgreSQL database 'ai_meeting_platform'."
        exit 1
    fi
    echo "  ✓ PostgreSQL database 'ai_meeting_platform' is reachable."
fi

if [ ! -d "$PROJECT_ROOT/ai-service/.venv" ]; then
    echo "ERROR: Virtual environment not found at $PROJECT_ROOT/ai-service/.venv"
    exit 1
fi

echo "  ✓ All dependency checks passed."

# 2. Check / Start AI Microservice (Port 8000)
echo "[2/6] Checking AI Service (Port 8000)..."
if healthy_ai; then
    echo "  ✓ Reusing healthy FastAPI service on $AI_URL"
else
    start_ai
    echo "  ✓ FastAPI service started and passed /health."
fi

# 3. Check / Start Spring Boot Backend (Port 8080)
echo "[3/6] Checking Spring Boot Backend (Port 8080)..."
if healthy_backend; then
    echo "  ✓ Reusing healthy Spring Boot backend on $BACKEND_URL"
else
    start_backend
    echo "  ✓ Spring Boot backend started and passed /health."
fi

# 4. Check / Start Cloudflare Tunnel
echo "[4/6] Checking Cloudflare Tunnel..."
TUNNEL_URL=""

# A URL recorded in a log is not proof that a tunnel still exists. Reuse it
# only after its public backend health endpoint answers successfully.
CANDIDATE_URL=$(grep -Eo 'https://[^[:space:]"|]+\.trycloudflare\.com' "$LOG_DIR/cloudflared.log" 2>/dev/null | tail -n 1 || true)
if [ -n "$CANDIDATE_URL" ]; then
    echo "  -> Verifying public tunnel URL: $CANDIDATE_URL ..."
    if healthy_tunnel "$CANDIDATE_URL"; then
        TUNNEL_URL="$CANDIDATE_URL"
        echo "  ✓ Existing Cloudflare tunnel is live and publicly healthy."
    fi
fi

# Step B: If no live tunnel is available, start a fresh quick tunnel. Do not
# kill another cloudflared process; it may be user-managed or serve another app.
if [ -z "$TUNNEL_URL" ]; then
    echo "  -> Starting fresh Cloudflare Tunnel..."
    > "$LOG_DIR/cloudflared.log"
    nohup cloudflared tunnel --protocol http2 --url "$BACKEND_URL" > "$LOG_DIR/cloudflared.log" 2>&1 &

    # Poll log for new URL
    for i in {1..15}; do
        CANDIDATE_URL=$(grep -Eo 'https://[^[:space:]"|]+\.trycloudflare\.com' "$LOG_DIR/cloudflared.log" 2>/dev/null | tail -n 1 || true)
        if [ -n "$CANDIDATE_URL" ] && healthy_tunnel "$CANDIDATE_URL"; then
            TUNNEL_URL="$CANDIDATE_URL"
            break
        fi
        sleep 2
    done

    if [ -z "$TUNNEL_URL" ]; then
        echo "ERROR: Failed to obtain a fresh Cloudflare Quick Tunnel URL. See log: $LOG_DIR/cloudflared.log"
        exit 1
    fi

    echo "  ✓ New Cloudflare tunnel is publicly healthy: $TUNNEL_URL"
fi

# 5. Check & Sync Vercel Production Environment
echo "[5/6] Verifying Vercel Frontend Connection..."
cd "$PROJECT_ROOT/frontend"

# `vercel env ls` does not reveal encrypted environment values, so retain the
# URL only after a successful deployment and use it to avoid needless deploys.
if [ -f "$TUNNEL_STATE_FILE" ] && [ "$(<"$TUNNEL_STATE_FILE")" = "$TUNNEL_URL" ]; then
    echo "  ✓ Production frontend was already deployed for the verified live tunnel."
else
    echo "  -> Tunnel URL changed or misaligned! Updating Vercel production environment to $TUNNEL_URL ..."
    if ! vercel --prod --yes \
        --env VITE_API_BASE_URL="$TUNNEL_URL" \
        --build-env VITE_API_BASE_URL="$TUNNEL_URL" > "$LOG_DIR/vercel-deploy.log" 2>&1; then
        echo "ERROR: Vercel deployment failed. Check log at: $LOG_DIR/vercel-deploy.log"
        exit 1
    fi
    printf '%s\n' "$TUNNEL_URL" > "$TUNNEL_STATE_FILE"
    echo "  ✓ Vercel production redeployment completed successfully."
fi

# 6. Final End-to-End System Health Checks
echo "[6/6] Final End-to-End System Health Checks..."

DB_OK=false
if psql -U vyankateshtalokar -d ai_meeting_platform -c "SELECT 1;" &> /dev/null; then
    DB_OK=true
fi

AI_OK=false; healthy_ai && AI_OK=true
BACKEND_OK=false; healthy_backend && BACKEND_OK=true
TUNNEL_OK=false; healthy_tunnel "$TUNNEL_URL" && TUNNEL_OK=true

FRONTEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$FRONTEND_URL" 2>/dev/null || true)
FRONTEND_OK=false
if [ "$FRONTEND_CODE" = "200" ]; then
    FRONTEND_OK=true
fi

echo "------------------------------------------------------------------"
echo "   SYSTEM HEALTH SUMMARY"
echo "------------------------------------------------------------------"
echo "   PostgreSQL Database                   : $( [ "$DB_OK" = true ] && echo "REACHABLE ✓" || echo "UNREACHABLE ❌" )"
echo "   Local FastAPI AI Service (Port 8000)   : $( [ "$AI_OK" = true ] && echo "HTTP 200 OK ('status':'ok') ✓" || echo "FAILED ❌" )"
echo "   Local Spring Boot Backend (Port 8080)   : $( [ "$BACKEND_OK" = true ] && echo "HTTP 200 OK ('status':'UP') ✓" || echo "FAILED ❌" )"
echo "   Cloudflare Public Tunnel ($TUNNEL_URL) : $( [ "$TUNNEL_OK" = true ] && echo "HTTP 200 OK ('status':'UP') ✓" || echo "FAILED ❌" )"
echo "   Vercel Public Frontend ($FRONTEND_URL) : $( [ "$FRONTEND_OK" = true ] && echo "HTTP 200 OK ✓" || echo "FAILED (HTTP $FRONTEND_CODE) ❌" )"
echo "------------------------------------------------------------------"

if [ "$DB_OK" = true ] && [ "$AI_OK" = true ] && [ "$BACKEND_OK" = true ] && [ "$TUNNEL_OK" = true ] && [ "$FRONTEND_OK" = true ]; then
    echo "STATUS: PRESENTATION READY 🚀"
    echo "Use this URL for your presentation: $FRONTEND_URL"
    echo "=================================================================="
else
    echo "ERROR: Presentation Readiness Check Failed! One or more required components are failing."
    [ "$DB_OK" = false ] && echo " - Layer Failed: PostgreSQL Database"
    [ "$AI_OK" = false ] && echo " - Layer Failed: FastAPI AI Service (http://localhost:8000/health)"
    [ "$BACKEND_OK" = false ] && echo " - Layer Failed: Spring Boot Backend (http://localhost:8080/health)"
    [ "$TUNNEL_OK" = false ] && echo " - Layer Failed: Cloudflare Public Tunnel ($TUNNEL_URL/health)"
    [ "$FRONTEND_OK" = false ] && echo " - Layer Failed: Vercel Public Frontend ($FRONTEND_URL)"
    echo "=================================================================="
    exit 1
fi
