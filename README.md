# Backend Junta de Vecinos Población Zaror (DSY1105)

Tres microservicios Java + Spring Boot 3 (Maven) que dan servicio REST a la app Android, según la
propuesta técnica del profesor (sección 4.3). Todos los datos son ficticios.

| Servicio | Puerto | Responsabilidad | Tablas propias |
|---|---|---|---|
| `ms-reservas` | 8081 | Dependencias, solicitudes y ciclo de vida de reservas; valida superposición de horarios | `dependencia`, `reserva`, `historial_reserva` |
| `ms-finanzas` | 8082 | Pagos informativos, ingresos, gastos, categorías, comprobantes e informes | `categoria_movimiento`, `movimiento_financiero`, `pago`, `comprobante`, `informe_financiero` |
| `ms-admin` | 8083 | Login, cuentas, perfiles y roles | `usuario`, `rol` |

Cada servicio solo toca sus tablas. Entre servicios solo viajan ids (`usuario_id`, `reserva_id`), nunca consultas
cruzadas a tablas ajenas. Hay una sola base de datos física (H2 en `~/jdv-data/jdv`) con las tablas separadas por
responsabilidad, como permite el MVP de la propuesta.

## Cómo está organizado cada servicio (igual que MVVM en la app)

`web` (controladores REST) → `servicio` (reglas de negocio) → `repositorio` (Spring Data JPA) → `modelo` (entidades).
Los datos de entrada y salida son `dto` (records) y las entidades nunca se exponen directamente.

## Ejecutar

Requisitos: Java 17 o superior (`java -version`) y Maven (`brew install maven`).

```bash
cd FelipeCortes_Backend
mvn test                # corre todas las pruebas de los 3 servicios
./run-all.sh            # levanta los 3 servicios (8083, luego 8081 y 8082)
./probar-api.sh         # demo con curl: login, reserva, aprobación, finanzas
```

Para levantar uno solo: `mvn -pl ms-admin spring-boot:run` (o `ms-reservas`, `ms-finanzas`).
Variables opcionales: `PORT`, `JDV_DB_URL`, `JDV_SECRETO` (el secreto debe ser el mismo en los 3 servicios).

## Cuentas de prueba (las crea ms-admin al iniciar si la base está vacía)

| Correo | Clave | Perfil |
|---|---|---|
| vecino@jdvzaror.cl | 1234 | VECINO |
| directiva@jdvzaror.cl | directiva123 | DIRECTIVA |
| tesoreria@jdvzaror.cl | tesoreria123 | TESORERIA |
| admin@jdvzaror.cl | admin123 | ADMIN |

Las contraseñas se guardan con hash BCrypt, nunca en texto plano.

## Autenticación y permisos (se aplican en el backend, no solo en la app)

`POST /api/auth/login` devuelve un token. Las demás llamadas llevan `Authorization: Bearer <token>`. Los 3 servicios
validan la firma del token con un secreto compartido; si falta o es inválido responden 401, y si el perfil no tiene
permiso responden 403. Cada endpoint declara sus roles con `@RequiereRol`.

## Endpoints principales

**ms-admin (8083)**

- `POST /api/auth/login`, `POST /api/auth/recuperar` (simulada, responde igual exista o no el correo), `GET /api/auth/yo`
- `GET|POST /api/usuarios`, `PUT /api/usuarios/{id}/estado`, `GET /api/roles` (solo ADMIN)

**ms-reservas (8081)**

- `GET /api/dependencias` (todos), `POST|PUT /api/dependencias` (ADMIN)
- `POST /api/reservas` (VECINO, ADMIN) · `GET /api/reservas/mias` · `GET /api/reservas?estado=` (DIRECTIVA, TESORERIA, ADMIN)
- `GET /api/reservas/disponibilidad?dependenciaId=&fecha=AAAA-MM-DD` (solo horarios ocupados, sin datos personales)
- `PUT /api/reservas/{id}/aprobar|rechazar` (DIRECTIVA, ADMIN) · `PUT /api/reservas/{id}/cancelar` (dueña o directiva)
- `GET /api/reservas/{id}/historial`

**ms-finanzas (8082)**

- `GET /api/categorias?tipo=`, `POST|PUT /api/categorias` (ADMIN)
- `POST /api/movimientos` (TESORERIA, ADMIN) · `GET /api/movimientos?periodo=AAAA-MM` (TESORERIA, DIRECTIVA, ADMIN) · `PUT /api/movimientos/{id}/anular`
- `POST|GET /api/movimientos/{id}/comprobantes` (solo metadatos; el archivo queda en el celular)
- `POST /api/pagos` (TESORERIA, ADMIN) crea el pago informativo y su ingreso asociado
- `GET /api/informes/resumen?periodo=AAAA-MM` (todos, incluidos vecinos: solo totales)
- `GET /api/informes/detalle?periodo=` (TESORERIA, DIRECTIVA, ADMIN) · `POST /api/informes` (TESORERIA, ADMIN)

## Reglas de negocio implementadas

- No se aprueban dos reservas aprobadas que se superpongan en la misma dependencia y fecha (se valida en el servidor al solicitar y al aprobar).
- La hora de término debe ser posterior a la de inicio y la fecha no puede ser pasada.
- Rechazar exige un motivo; solo se resuelven solicitudes pendientes; cada cambio de estado queda en `historial_reserva`.
- Los montos deben ser positivos, y la categoría debe coincidir con el tipo (ingreso o gasto).
- Los movimientos no se borran: se anulan, y los anulados no cuentan en los totales; anular un ingreso anula su pago.
- Una reserva solo puede tener un pago informativo vigente (no se cuenta dos veces).
- Los vecinos solo ven el resumen (ingresos, gastos, saldo): sin movimientos, observaciones ni comprobantes.
- `claveCliente` (opcional) evita duplicar reservas o movimientos si el celular reintenta el envío.

## Decisiones de alcance (documentadas, como pide la propuesta)

- Autenticación simplificada: token firmado con HMAC-SHA256 y secreto compartido. No es un JWT ni una solución productiva.
- Recuperación de contraseña simulada: no envía correos.
- Los pagos son solo informativos; no hay pasarelas ni bancos.
- `ms-finanzas` guarda el `reserva_id` pero no verifica que exista en `ms-reservas` (no se accede a tablas ajenas).
- Los comprobantes guardan solo metadatos y ruta de referencia.
- Base H2 en archivo para que corra sin instalar nada; basta cambiar `JDV_DB_URL` y el driver para usar MySQL o PostgreSQL.

## Pendiente

Conectar la app Android con estos servicios usando Retrofit/OkHttp (el emulador accede al computador en `http://10.0.2.2:8081`, `:8082` y `:8083`).
