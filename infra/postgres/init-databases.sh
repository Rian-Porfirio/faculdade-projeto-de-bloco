#!/bin/bash
# Cria os 3 bancos (um por serviço) na primeira subida do container PostgreSQL.
set -e
for DB in votacao resultados auditoria; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-SQL
    SELECT 'CREATE DATABASE $DB' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB')\gexec
SQL
done
