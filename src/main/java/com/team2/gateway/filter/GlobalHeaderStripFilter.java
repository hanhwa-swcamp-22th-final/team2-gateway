package com.team2.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 외부에서 유입된 신뢰 헤더(X-User-*, X-Internal-Token)를 다운스트림 전달 전에 제거.
 *
 * 클라이언트가 임의로 X-User-Id 등을 전송해 백엔드 JwtAuthFilter 의
 * "헤더 신뢰" 분기를 악용하는 Header Spoofing 공격을 차단한다.
 *
 * Ordered.HIGHEST_PRECEDENCE 로 등록해 모든 필터보다 먼저 실행된다.
 */
@Component
public class GlobalHeaderStripFilter implements GlobalFilter, Ordered {

    private static final List<String> HEADERS_TO_STRIP = List.of(
            "X-User-Id",
            "X-User-Email",
            "X-User-Name",
            "X-User-Role",
            "X-User-Department-Id",
            "X-Internal-Token"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> HEADERS_TO_STRIP.forEach(headers::remove))
                .build();

        return chain.filter(exchange.mutate().request(sanitizedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
