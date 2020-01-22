#!/bin/sh

echo "Starting postgres container"
docker run --rm --name postgres -p5432:5432 -e POSTGRES_PASSWORD=testpassword -e POSTGRES_USER=testuser -d postgres:12.1

echo "Waiting Postgres to launch ..."
#while ! pg_isready -h localhost -U testuser >/dev/null; do
while ! docker exec postgres pg_isready -h localhost -U testuser    ; do
    sleep 0.1
done

echo "Initialize table"
export PGPASSWORD=testpassword
psql -h localhost -U testuser -f db_init.sql
