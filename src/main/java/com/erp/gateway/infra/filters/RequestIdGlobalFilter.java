package com.erp.gateway.infra.filters;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

  public static final String HEADER = "X-Request-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
    String requestId = exchange.getRequest().getHeaders().getFirst(HEADER);
    if (!StringUtils.hasText(requestId)) {
      requestId = UUID.randomUUID().toString();
      ServerHttpRequest mutated = exchange.getRequest().mutate()
          .header(HEADER, requestId)
          .build();
      exchange = exchange.mutate().request(mutated).build();
    }

    // También lo devolvemos al cliente para trazabilidad end-to-end
    exchange.getResponse().getHeaders().set(HEADER, requestId);

    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    // Muy temprano en la cadena
    return -1000;
  }
}