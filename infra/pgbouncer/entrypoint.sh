#!/bin/sh
set -eu

umask 077
auth_file=/tmp/pgbouncer-userlist.txt
printf '"%s" "%s"\n' "$DATABASES_USER" "$DATABASES_PASSWORD" > "$auth_file"
export PGBOUNCER_AUTH_FILE="$auth_file"

exec /opt/pgbouncer/entrypoint.sh
