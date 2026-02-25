package com.erp.gateway.infra.filters;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class ServerRequestLogWebFilter implements WebFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(ServerRequestLogWebFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    final String path = exchange.getRequest().getURI().getPath();

    if (!shouldLog(path)) {
      return chain.filter(exchange);
    }

    String requestId = exchange.getRequest().getHeaders().getFirst(RequestIdGlobalFilter.HEADER);
    ServerWebExchange mutableExchange = exchange;

    if (!StringUtils.hasText(requestId)) {
      requestId = UUID.randomUUID().toString();
      ServerHttpRequest mutated = exchange.getRequest().mutate()
          .header(RequestIdGlobalFilter.HEADER, requestId)
          .build();
      mutableExchange = exchange.mutate().request(mutated).build();
    }

    final ServerWebExchange ex = mutableExchange;
    ex.getResponse().getHeaders().set(RequestIdGlobalFilter.HEADER, requestId);

    final String rid = requestId;
    final String method = ex.getRequest().getMethod() != null
        ? ex.getRequest().getMethod().name()
        : "UNKNOWN";
    final long start = System.currentTimeMillis();

    return chain.filter(ex)
        .doOnSubscribe(s ->
            log.info("server_request_start requestId={} method={} path={}", rid, method, path)
        )
        .doOnSuccess(v -> {
          Integer status = ex.getResponse().getStatusCode() != null
              ? ex.getResponse().getStatusCode().value()
              : 0;
          long ms = System.currentTimeMillis() - start;
          log.info("server_request_end requestId={} status={} durationMs={} method={} path={}",
              rid, status, ms, method, path);
        })
        .doOnError(err -> {
          long ms = System.currentTimeMillis() - start;
          log.error("server_request_error requestId={} durationMs={} method={} path={} error={}",
              rid, ms, method, path, err.toString());
        });
  }

  private boolean shouldLog(String path) {
    return path.startsWith("/actuator/")
        || "/health".equals(path)
        || "/prometheus".equals(path)
        || path.startsWith("/test");
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
