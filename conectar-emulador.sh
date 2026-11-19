#!/bin/bash
# Conecta el emulador con los 3 microservicios del computador (repetir si se reinicia el emulador)
ADB=~/Library/Android/sdk/platform-tools/adb
for puerto in 8081 8082 8083; do
  $ADB reverse tcp:$puerto tcp:$puerto
done
echo "Emulador conectado a los microservicios"
