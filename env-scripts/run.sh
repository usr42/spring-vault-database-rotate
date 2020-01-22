#!/bin/sh

echo '### Stopping environment'
./stop.sh
echo '### Starting vault'
./start_vault.sh &

echo "Waiting Vault to launch on 8080..."
while ! nc -z localhost 8200; do
    sleep 0.1
done

echo '### Initialize the DB'
./init_db.sh
echo '### Configure the database secrets plugin in vault'
./configure_db_in_vault.sh
