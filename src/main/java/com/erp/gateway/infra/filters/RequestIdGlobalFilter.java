package com.erp.gateway.infra.filters;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(RequestIdGlobalFilter.class);
  public static final String HEADER = "X-Request-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange,
                          org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

    String requestId = exchange.getRequest().getHeaders().getFirst(HEADER);

    ServerWebExchange mutableExchange = exchange;

    if (!StringUtils.hasText(requestId)) {
      requestId = UUID.randomUUID().toString();

      ServerHttpRequest mutated = exchange.getRequest().mutate()
          .header(HEADER, requestId)
          .build();

      mutableExchange = exchange.mutate().request(mutated).build();
    }

    final ServerWebExchange ex = mutableExchange;

    // devolver requestId al cliente (siempre)
    mutableExchange.getResponse().getHeaders().set(HEADER, requestId);

    final String rid = requestId;
    final String method = ex.getRequest().getMethod() != null
        ? ex.getRequest().getMethod().name()
        : "UNKNOWN";
    final String path = ex.getRequest().getURI().getPath();

    final long start = System.currentTimeMillis();

    return chain.filter(ex)
        .doOnSubscribe(s ->
            log.info("gateway_request_start requestId={} method={} path={}", rid, method, path)
        )
        .doOnSuccess(v -> {
          Integer status = ex.getResponse().getStatusCode() != null
              ? ex.getResponse().getStatusCode().value()
              : 0;
          long ms = System.currentTimeMillis() - start;

          log.info("gateway_request_end requestId={} status={} durationMs={} method={} path={}",
              rid, status, ms, method, path);
        })
        .doOnError(err -> {
          long ms = System.currentTimeMillis() - start;

          log.error("gateway_request_error requestId={} durationMs={} method={} path={} error={}",
              rid, ms, method, path, err.toString());
        });
  }

  @Override
  public int getOrder() {
    return -1000;
  }
}