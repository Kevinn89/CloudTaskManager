#!/bin/bash

set -e

# ============================================================
# Defaults
# ============================================================

APP_PORT=8080
SPRING_PROFILE="local"
RUN_TESTS=false
RUN_APP=true
POSTGRES_SERVICE="postgres"

# ============================================================
# Usage
# ============================================================

usage() {
  echo ""
  echo "Usage:"
  echo "  ./run-local.sh [options]"
  echo ""
  echo "Options:"
  echo "  --port <port>           Port to kill and run Spring Boot on. Default: 8080"
  echo "  --run-tests <true|false> Run all tests during build. Default: false"
  echo "  --run-app <true|false>   Run app after build. Default: true"
  echo "  --profile <profile>      Spring profile. Default: local"
  echo ""
  echo "Examples:"
  echo "  ./run-local.sh"
  echo "  ./run-local.sh --port 8081"
  echo "  ./run-local.sh --run-tests true"
  echo "  ./run-local.sh --port 8081 --run-tests true"
  echo "  ./run-local.sh --run-app false"
  echo ""
}

# ============================================================
# Parse args
# ============================================================

while [[ $# -gt 0 ]]; do
  case "$1" in
    --port)
      APP_PORT="$2"
      shift 2
      ;;
    --run-tests)
      RUN_TESTS="$2"
      shift 2
      ;;
    --run-app)
      RUN_APP="$2"
      shift 2
      ;;
    --profile)
      SPRING_PROFILE="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

# ============================================================
# Validation
# ============================================================

if ! [[ "$APP_PORT" =~ ^[0-9]+$ ]]; then
  echo "Invalid port: $APP_PORT"
  exit 1
fi

if [[ "$RUN_TESTS" != "true" && "$RUN_TESTS" != "false" ]]; then
  echo "Invalid --run-tests value: $RUN_TESTS"
  echo "Use true or false."
  exit 1
fi

if [[ "$RUN_APP" != "true" && "$RUN_APP" != "false" ]]; then
  echo "Invalid --run-app value: $RUN_APP"
  echo "Use true or false."
  exit 1
fi

# ============================================================
# Functions
# ============================================================

kill_port() {
  local port=$1

  echo "Checking port $port..."

  local pids
  pids=$(lsof -ti tcp:"$port" || true)

  if [[ -n "$pids" ]]; then
    echo "Killing process using port $port: $pids"
    kill -9 $pids
  else
    echo "No process using port $port"
  fi
}

start_postgres() {
  echo "Starting Postgres container..."

  docker compose up -d "$POSTGRES_SERVICE"

  echo "Waiting for Postgres to be ready..."

  local retries=30
  local count=0

  until docker compose exec -T "$POSTGRES_SERVICE" pg_isready -U postgres > /dev/null 2>&1; do
    count=$((count + 1))

    if [[ "$count" -ge "$retries" ]]; then
      echo "Postgres did not become ready in time."
      exit 1
    fi

    echo "Postgres not ready yet... retrying ($count/$retries)"
    sleep 2
  done

  echo "Postgres is ready."
}

build_app() {
  echo "Building Spring Boot app..."

  if [[ "$RUN_TESTS" == "true" ]]; then
    echo "Running all tests, including integration tests..."
    ./gradlew clean build
  else
    echo "Skipping all tests..."
    ./gradlew clean build -x test
  fi
}

run_app() {
  echo "Starting Spring Boot app..."
  echo "Profile: $SPRING_PROFILE"
  echo "Port: $APP_PORT"

  ./gradlew bootRun --args="--spring.profiles.active=$SPRING_PROFILE --server.port=$APP_PORT"
}

# ============================================================
# Main
# ============================================================

echo "Local startup config:"
echo "  Port:       $APP_PORT"
echo "  Profile:    $SPRING_PROFILE"
echo "  Run tests:  $RUN_TESTS"
echo "  Run app:    $RUN_APP"
echo ""

start_postgres
kill_port "$APP_PORT"
build_app

if [[ "$RUN_APP" == "true" ]]; then
  run_app
else
  echo "Build complete. App was not started because --run-app=false."
fi