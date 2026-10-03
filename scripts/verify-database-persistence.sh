#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f .env ]]; then
  echo "Missing .env. Copy .env.example to .env and set local-only passwords first." >&2
  exit 1
fi

docker compose up -d --wait postgres pgbouncer

before_restart="$(docker compose exec -T postgres sh -c \
  'PGPASSWORD="$APP_DATABASE_PASSWORD" psql -h pgbouncer -p 6432 -U "$APP_DATABASE_USER" -d "$POSTGRES_DB" -Atc "select count(*) from flyway_schema_history where success"')"

if [[ "$before_restart" -lt 1 ]]; then
  echo "No successful Flyway migration found. Start the API once before running this check." >&2
  exit 1
fi

docker compose stop postgres pgbouncer
docker compose up -d --wait postgres pgbouncer

after_restart="$(docker compose exec -T postgres sh -c \
  'PGPASSWORD="$APP_DATABASE_PASSWORD" psql -h pgbouncer -p 6432 -U "$APP_DATABASE_USER" -d "$POSTGRES_DB" -Atc "select count(*) from flyway_schema_history where success"')"

if [[ "$after_restart" != "$before_restart" ]]; then
  echo "Migration history changed across container recreation: before=$before_restart after=$after_restart" >&2
  exit 1
fi

echo "Database persistence verified: $after_restart successful migration(s) survived container recreation."
