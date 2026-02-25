package com.erp.gateway.infra.filters;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
        .doOnSubscribe(s -> withRequestMdc(rid, method, path, null, null, null,
            () -> log.info("gateway_request_start")))
        .doOnSuccess(v -> {
          Integer status = ex.getResponse().getStatusCode() != null
              ? ex.getResponse().getStatusCode().value()
              : 0;
          long ms = System.currentTimeMillis() - start;
          withRequestMdc(rid, method, path, String.valueOf(status), String.valueOf(ms), null,
              () -> log.info("gateway_request_end"));
        })
        .doOnError(err -> {
          long ms = System.currentTimeMillis() - start;
          withRequestMdc(rid, method, path, null, String.valueOf(ms), err.getClass().getSimpleName(),
              () -> log.error("gateway_request_error", err));
        });
  }

  private static void withRequestMdc(String requestId, String method, String path, String httpStatus,
                                     String durationMs, String error, Runnable action) {
    String prevRequestId = MDC.get("requestId");
    String prevMethod = MDC.get("httpMethod");
    String prevPath = MDC.get("httpPath");
    String prevStatus = MDC.get("httpStatus");
    String prevDuration = MDC.get("durationMs");
    String prevError = MDC.get("error");

    putOrRemove("requestId", requestId);
    putOrRemove("httpMethod", method);
    putOrRemove("httpPath", path);
    putOrRemove("httpStatus", httpStatus);
    putOrRemove("durationMs", durationMs);
    putOrRemove("error", error);
    try {
      action.run();
    } finally {
      putOrRemove("requestId", prevRequestId);
      putOrRemove("httpMethod", prevMethod);
      putOrRemove("httpPath", prevPath);
      putOrRemove("httpStatus", prevStatus);
      putOrRemove("durationMs", prevDuration);
      putOrRemove("error", prevError);
    }
  }

  private static void putOrRemove(String key, String value) {
    if (value == null) {
      MDC.remove(key);
      return;
    }
    MDC.put(key, value);
  }

  @Override
  public int getOrder() {
    return -1000;
  }
}
