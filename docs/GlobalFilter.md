📍 Ubicación
com.erp.gateway.infra.filters.RequestIdGlobalFilter
🎯 Propósito

Este filtro global garantiza que todas las peticiones que atraviesan el API Gateway tengan un identificador único (X-Request-Id) para:

Trazabilidad end-to-end

Correlación de logs entre microservicios

Debug y auditoría

Observabilidad en sistemas distribuidos

El Gateway es el punto único de entrada al ERP, por lo que es el lugar correcto para generar y propagar este identificador.

🧱 Tipo de filtro
public class RequestIdGlobalFilter implements GlobalFilter, Ordered

GlobalFilter → Se ejecuta en todas las rutas

Ordered → Permite definir prioridad en la cadena de filtros

@Override
public int getOrder() {
  return -1000;
}

Un valor negativo alto significa:

Se ejecuta muy pronto en el pipeline.

Esto garantiza que todos los filtros posteriores y microservicios reciban el X-Request-Id.

🔁 Flujo de ejecución
1️⃣ Leer header entrante
String requestId = exchange.getRequest().getHeaders().getFirst(HEADER);

Si el cliente ya envía X-Request-Id → se respeta.

Si no existe → se genera uno nuevo.

2️⃣ Generación de UUID (si no existe)
requestId = UUID.randomUUID().toString();

Se crea un identificador único global.

Luego se muta la request:

ServerHttpRequest mutated = exchange.getRequest().mutate()
    .header(HEADER, requestId)
    .build();

En WebFlux las requests son inmutables, por lo que se debe crear una nueva.

3️⃣ Propagación al downstream
mutableExchange = exchange.mutate().request(mutated).build();

Se crea un nuevo ServerWebExchange con la request modificada.

Esto asegura que:

Auth Service

Sales Service

Cualquier microservicio futuro

reciban el mismo X-Request-Id.

4️⃣ Devolver el header al cliente
mutableExchange.getResponse().getHeaders().set(HEADER, requestId);

Esto permite:

Correlacionar respuesta con petición

Ver el ID en herramientas como Postman o navegador

Diagnóstico rápido en frontend

📊 Logging estructurado (Access Log del Gateway)

Se capturan los siguientes datos:

requestId

método HTTP

path

status HTTP

duración en milisegundos

Inicio de petición
log.info("gateway_request_start requestId={} method={} path={}", ...)

Permite saber cuándo entra la request al Gateway.

Fin de petición
log.info("gateway_request_end requestId={} status={} durationMs={} method={} path={}", ...)

Permite medir:

Código de estado

Tiempo de respuesta

Ruta afectada

Esto es esencial para:

Métricas futuras

Análisis de latencia

Detección de cuellos de botella

Manejo de errores
log.error("gateway_request_error requestId={} ...", ...)

Registra errores no controlados.

Permite rastrear exactamente qué petición falló.

🧠 Por qué usar ServerWebExchange mutable

En WebFlux:

Las requests son inmutables

No se puede modificar directamente el objeto original

Se debe crear una versión mutada

Por eso se usa:

exchange.mutate()
🏗 Rol en la arquitectura del ERP

Este filtro implementa:

Trazabilidad centralizada

Correlación distribuida básica

Base para integración futura con:

OpenTelemetry

Zipkin

Jaeger

ELK / Loki

Es el primer paso hacia observabilidad distribuida.

🔮 Evolución futura

Este diseño permite evolucionar a:

MDC contextual automático

Micrometer Tracing

Propagación W3C Trace Context

Integración con Prometheus y Grafana

Sin necesidad de reescribir el Gateway.

🧾 Resumen

Este filtro:

Garantiza que toda petición tenga X-Request-Id

Lo propaga a microservicios

Lo devuelve al cliente

Genera logs de acceso estructurados

Es el punto base de la observabilidad del ERP