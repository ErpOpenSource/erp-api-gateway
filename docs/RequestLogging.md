# Request Logging Model

## Causa

`RequestIdGlobalFilter` es un `GlobalFilter` de Spring Cloud Gateway.
Se ejecuta en la cadena de routing cuando la peticion coincide con rutas del gateway
(por ejemplo `/auth/**`, `/sales/**`).

Los endpoints de Actuator (por ejemplo `/actuator/health`, `/actuator/prometheus`)
se sirven desde el pipeline web del servidor y no se resuelven como rutas del Gateway.
Por eso no pasan por los filtros de routing de Spring Cloud Gateway (`GlobalFilter`).

## Solucion

Se agrega `ServerRequestLogWebFilter` para interceptar peticiones locales del servidor
(especialmente Actuator, y tambien requests como `/test` que no estan ruteadas por Gateway).

Este filtro:

- Genera y propaga `X-Request-Id` en `/actuator/**`.
- Emite logs `server_request_start`, `server_request_end`, `server_request_error`.
- Permite trazabilidad entre endpoints ruteados por Gateway y endpoints locales del servidor.

## Validacion

Compila con:

```powershell
.\mvnw -DskipTests compile -q
```

Pruebas sugeridas:

- `GET /actuator/health`
- `GET /actuator/prometheus`
- `GET /test` (404 esperado, pero logueado)
