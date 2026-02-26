# erp-api-gateway

Enterprise API Gateway for ERP microservices architecture. Built with **Spring Cloud Gateway** (WebFlux/reactive), it acts as the single entry point for all incoming requests, providing routing, tracing, structured logging, and observability integration.

> Part of the [ErpOpenSource Observability Platform](https://github.com/ErpOpenSource/erp-observability-platform)

---

## Overview

```
Client
  │
  ▼
erp-api-gateway  :8080
  ├── /auth/**   → auth-service   :8081
  ├── /sales/**  → sales-service  :8082
  └── /actuator  → (local – health, metrics, prometheus)
```

**Key capabilities:**

- Reactive, non-blocking routing via Spring WebFlux
- `X-Request-Id` propagation for distributed tracing
- Structured JSON logging with MDC context
- OpenTelemetry-ready with OTLP exporter
- Prometheus metrics endpoint
- CORS global configuration
- Docker multi-stage build + external `erp-platform` network
- GitHub Actions CI pipeline

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.3.5 |
| Gateway | Spring Cloud Gateway 2023.0.6 (WebFlux) |
| Observability | Micrometer + OpenTelemetry (OTLP) |
| Metrics | Prometheus (via Micrometer) |
| Logging | Logback + Logstash JSON encoder |
| Build | Maven 3.9.9 |
| Container | Docker (multi-stage, eclipse-temurin:21-jre) |

---

## Project Structure

```
erp-api-gateway/
├── src/
│   └── main/
│       ├── java/com/erp/gateway/
│       │   ├── ErpApiGatewayApplication.java
│       │   └── infra/filters/
│       │       ├── RequestIdGlobalFilter.java   # X-Request-Id propagation + MDC
│       │       └── ServerRequestLogWebFilter.java  # Logging for Actuator endpoints
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
├── docs/
│   ├── GlobalFilter.md
│   ├── RequestLogging.md
│   └── Cors.md
├── .github/workflows/ci.yml
├── Dockerfile
└── docker-compose.yml
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+ (or use the included `mvnw`)
- Docker (for containerized runs)

### Run locally

```bash
./mvnw spring-boot:run
```

Gateway will start on `http://localhost:8080`.

### Build

```bash
./mvnw clean package
java -jar target/erp-api-gateway-*.jar
```

### Run with Docker Compose

```bash
docker compose up --build
```

> Requires an external Docker network named `erp-platform`:
> ```bash
> docker network create erp-platform
> ```

---

## Configuration

Key settings in [src/main/resources/application.yml](src/main/resources/application.yml):

```yaml
server:
  port: 8080

spring:
  application:
    name: erp-api-gateway
  reactor:
    context-propagation: auto          # Reactor context for tracing
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8081
          predicates: [Path=/auth/**]
          filters: [StripPrefix=1]
        - id: sales-service
          uri: http://localhost:8082
          predicates: [Path=/sales/**]
          filters: [StripPrefix=1]

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  tracing:
    sampling:
      probability: 1.0    # 100% in dev – reduce for production
```

---

## Filters

### `RequestIdGlobalFilter` (order: -1000)

Executed on every proxied request:

1. Reads the incoming `X-Request-Id` header, or generates a new UUID.
2. Injects `requestId` into MDC for correlated JSON logs.
3. Forwards the header to the downstream service.
4. Adds `X-Request-Id` to the response.
5. Logs `gateway_request_start` / `gateway_request_end` (method, path, status, duration).

### `ServerRequestLogWebFilter` (order: `HIGHEST_PRECEDENCE`)

Handles requests served locally (Actuator, health probes). Follows the same MDC + JSON logging pattern without going through the gateway routing chain.

See [docs/GlobalFilter.md](docs/GlobalFilter.md) and [docs/RequestLogging.md](docs/RequestLogging.md) for full details.

---

## Observability

This service integrates with the [ErpOpenSource Observability Platform](https://github.com/ErpOpenSource/erp-observability-platform).

### Logs

Structured JSON logs via Logstash encoder. Every log line includes:

```json
{
  "timestamp": "2026-02-26T10:00:00.000Z",
  "level": "INFO",
  "message": "gateway_request_end",
  "service": "erp-api-gateway",
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "httpMethod": "POST",
  "httpPath": "/auth/login",
  "httpStatus": "200",
  "durationMs": "42"
}
```

### Traces

OpenTelemetry traces exported via OTLP. Configure the collector endpoint:

```yaml
management:
  otlp:
    tracing:
      endpoint: http://<otel-collector>:4318/v1/traces
```

### Metrics

Prometheus-format metrics available at:

```
GET http://localhost:8080/actuator/prometheus
```

### Health Probes

```
GET http://localhost:8080/actuator/health
GET http://localhost:8080/actuator/health/liveness
GET http://localhost:8080/actuator/health/readiness
```

---

## CORS

CORS is fully permissive in development (`allowedOrigins: *`).
**Must be restricted to explicit domains before deploying to production.**
See [docs/Cors.md](docs/Cors.md).

---

## CI/CD

GitHub Actions workflow ([.github/workflows/ci.yml](.github/workflows/ci.yml)) runs on every push and pull request to `main`:

1. Checkout code
2. Set up JDK 21 (Temurin)
3. Cache Maven dependencies
4. Run `./mvnw -B clean test`

---

## Related Repositories

| Repository | Description |
|---|---|
| [erp-observability-platform](https://github.com/ErpOpenSource/erp-observability-platform) | Centralized observability stack (Grafana, Loki, Tempo, Prometheus) |

---

## License

MIT License — see [LICENSE](LICENSE).
