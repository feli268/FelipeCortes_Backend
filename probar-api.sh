#!/usr/bin/env bash
# Demo con curl (requiere los 3 servicios levantados con ./run-all.sh).
A=http://localhost:8083; R=http://localhost:8081; F=http://localhost:8082
token() { curl -s -X POST $A/api/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])'; }
campo() { python3 -c "import sys,json; print(json.load(sys.stdin)[\"$1\"])"; }
VEC=$(token vecino@jdvzaror.cl 1234)
DIR=$(token directiva@jdvzaror.cl directiva123)
TES=$(token tesoreria@jdvzaror.cl tesoreria123)
H='Content-Type: application/json'

echo "== Vecino solicita la sede social (pendiente)"
ID=$(curl -s -X POST $R/api/reservas -H "Authorization: Bearer $VEC" -H "$H" \
  -d '{"dependenciaId":1,"fecha":"2030-05-10","horaInicio":"10:00","horaFin":"12:00","motivo":"Cumpleanos"}' | campo id)
echo "reserva $ID"
echo "== Directiva aprueba"
curl -s -X PUT $R/api/reservas/$ID/aprobar -H "Authorization: Bearer $DIR" -H "$H" -d '{"observacion":"Aprobada"}'; echo
echo "== Otro horario que se cruza: se rechaza (409)"
curl -s -o /dev/null -w "%{http_code}\n" -X POST $R/api/reservas -H "Authorization: Bearer $VEC" -H "$H" \
  -d '{"dependenciaId":1,"fecha":"2030-05-10","horaInicio":"11:00","horaFin":"13:00","motivo":"Otra fiesta"}'
echo "== Vecino intenta ver movimientos (403)"
curl -s -o /dev/null -w "%{http_code}\n" $F/api/movimientos -H "Authorization: Bearer $VEC"
echo "== Tesoreria registra el pago informativo de la reserva"
curl -s -X POST $F/api/pagos -H "Authorization: Bearer $TES" -H "$H" \
  -d "{\"reservaId\":$ID,\"monto\":60000,\"fechaPago\":\"2030-05-01\",\"medioInformado\":\"Transferencia\"}"; echo
echo "== Resumen que ve el vecino"
curl -s "$F/api/informes/resumen" -H "Authorization: Bearer $VEC"; echo
