# ─── RAG AI Project — Task Runner ────────────────────────────────────────────
# Requires: just (https://github.com/casey/just)
#           Docker Desktop
# Usage: just <recipe>

set dotenv-load
set windows-shell := ["cmd.exe", "/c"]

# ─── Paths ───────────────────────────────────────────────────────────────────
DOCKER_DIR  := "infrastructure/docker"
PROJECT_DIR := "."
ENV_FILE    := ".env"

# ─── Compose file sets ───────────────────────────────────────────────────────
CORE_CFG := "-f " + DOCKER_DIR + "/docker-compose.core.yml"
OBS_CFG  := "-f " + DOCKER_DIR + "/docker-compose.observability.yml"
TEST_CFG := "-f " + DOCKER_DIR + "/docker-compose.test.yml"

# ─── Base compose command ─────────────────────────────────────────────────────
COMPOSE := "docker compose --project-directory " + PROJECT_DIR + " --env-file " + ENV_FILE

# ─── Default recipe ───────────────────────────────────────────────────────────
default:
    @just --list

# ─────────────────────────────────────────────────────────────────────────────
# CORE
# ─────────────────────────────────────────────────────────────────────────────

# Start core services (postgres, minio, redpanda, keycloak, doc-parser, rag-service)
up-core:
    {{COMPOSE}} {{CORE_CFG}} up -d
    @echo Core services started

# Stop core services
down-core:
    {{COMPOSE}} {{CORE_CFG}} down

# Rebuild and restart core services
rebuild-core:
    {{COMPOSE}} {{CORE_CFG}} build --no-cache
    {{COMPOSE}} {{CORE_CFG}} up -d --force-recreate

# ─────────────────────────────────────────────────────────────────────────────
# OBSERVABILITY
# ─────────────────────────────────────────────────────────────────────────────

# Start observability stack (tempo, loki, alloy, prometheus, grafana)
up-observability: up-core
    {{COMPOSE}} {{OBS_CFG}} up -d
    @echo Observability stack started - Grafana: http://localhost:3000

# Stop observability stack
down-observability:
    {{COMPOSE}} {{OBS_CFG}} down

# ─────────────────────────────────────────────────────────────────────────────
# TEST GENERATORS
# ─────────────────────────────────────────────────────────────────────────────

# Start test load generators (tracegen, loggen, vulture)
up-test: up-core up-observability
    {{COMPOSE}} {{TEST_CFG}} up -d
    @echo Test generators started

# Stop test generators only
down-test:
    {{COMPOSE}} {{TEST_CFG}} down

# ─────────────────────────────────────────────────────────────────────────────
# DEV WORKFLOW
# ─────────────────────────────────────────────────────────────────────────────

# Start everything for development (core + observability)
dev: up-core up-observability
    @echo Dev environment ready
    @echo   App:          http://localhost:8080
    @echo   Grafana:      http://localhost:3000
    @echo   Keycloak:     http://localhost:8081
    @echo   Minio:        http://localhost:9001
    @echo   Redpanda UI:  http://localhost:8082
    @echo   Prometheus:   http://localhost:9090

# Start everything including test generators
dev-full: up-core up-observability up-test
    @echo Full dev environment (with test generators) ready

# ─────────────────────────────────────────────────────────────────────────────
# TEARDOWN
# ─────────────────────────────────────────────────────────────────────────────

# Stop all services
down:
    {{COMPOSE}} {{CORE_CFG}} {{OBS_CFG}} {{TEST_CFG}} down

# Stop all services and remove volumes + orphans
down-remove:
    {{COMPOSE}} {{CORE_CFG}} {{OBS_CFG}} {{TEST_CFG}} down --volumes --remove-orphans
    @echo All containers and volumes removed

# ─────────────────────────────────────────────────────────────────────────────
# LOGS
# ─────────────────────────────────────────────────────────────────────────────

# Follow all logs
logs:
    {{COMPOSE}} {{CORE_CFG}} {{OBS_CFG}} logs -f

# Follow core logs only
logs-core:
    {{COMPOSE}} {{CORE_CFG}} logs -f

# Follow observability logs
logs-obs:
    {{COMPOSE}} {{OBS_CFG}} logs -f

# Logs for a specific service: just log rag-service
log service:
    {{COMPOSE}} {{CORE_CFG}} {{OBS_CFG}} logs -f {{service}}

# ─────────────────────────────────────────────────────────────────────────────
# STATUS
# ─────────────────────────────────────────────────────────────────────────────

# Show running containers
ps:
    {{COMPOSE}} {{CORE_CFG}} {{OBS_CFG}} {{TEST_CFG}} ps

# Show resource usage
stats:
    docker stats --no-stream

# ─────────────────────────────────────────────────────────────────────────────
# BUILD
# ─────────────────────────────────────────────────────────────────────────────

# Build rag-service image only
build-rag:
    {{COMPOSE}} {{CORE_CFG}} build rag-service

# Build doc-parser image only
build-parser:
    {{COMPOSE}} {{CORE_CFG}} build doc-parser

# Build all images
build:
    {{COMPOSE}} {{CORE_CFG}} build

# ─────────────────────────────────────────────────────────────────────────────
# UTILS
# ─────────────────────────────────────────────────────────────────────────────

# Open Grafana in browser
[unix]
grafana:
    open http://localhost:3000
[windows]
grafana:
    start http://localhost:3000

# Open Minio console in browser
[unix]
minio:
    open http://localhost:9001
[windows]
minio:
    start http://localhost:9001

# Open Redpanda console in browser
[unix]
redpanda:
    open http://localhost:8082
[windows]
redpanda:
    start http://localhost:8082

# Copy .env.example to .env if not exists
[unix]
init:
    cp -n .env.example .env
    @echo .env created - edit it before running 'just dev'
[windows]
init:
    if not exist .env copy .env.example .env
    @echo .env created - edit it before running just dev
