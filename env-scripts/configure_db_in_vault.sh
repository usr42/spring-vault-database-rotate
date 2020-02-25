#!/bin/sh

. ../vault.env

echo '### Enabling db plugin'
vault secrets enable database
echo '### Configuring postgres'
vault write database/config/postgres plugin_name=postgresql-database-plugin allowed_roles="readonly" \
    connection_url="postgresql://{{username}}:{{password}}@localhost:5432/?sslmode=disable" username="testuser" \
    password="testpassword"
echo '### Create "readonly" db role'
vault write database/roles/readonly db_name=postgres \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
    GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" default_ttl="15" max_ttl="30"