#!/usr/bin/env bash
# Levanta los 3 microservicios. ms-admin primero porque crea los roles y cuentas de prueba.
set -e
cd "$(dirname "$0")"
mvn -q -DskipTests package
mkdir -p logs
for s in admin reservas finanzas; do
  echo "Iniciando ms-$s ..."
  java -jar "ms-$s/target/ms-$s-1.0.0.jar" > "logs/ms-$s.log" 2>&1 &
  echo $! >> logs/pids.txt
  sleep 8
done
echo "Listos: reservas http://localhost:8081  finanzas http://localhost:8082  admin http://localhost:8083"
echo "Para detenerlos: ./detener.sh"
