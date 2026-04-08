package com.team2.gateway.config;

import com.team2.gateway.security.JwtRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtRoleConverter jwtRoleConverter;

    public SecurityConfig(JwtRoleConverter jwtRoleConverter) {
        this.jwtRoleConverter = jwtRoleConverter;
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        // role 클레임 -> ROLE_* GrantedAuthority
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> Flux.fromIterable(jwtRoleConverter.convert(jwt))
        );
        return converter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // -- 공개 경로 (인증 불필요) -----------------------------------
                .pathMatchers(
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/forgot-password",
                    "/.well-known/jwks.json",
                    "/actuator/health"
                ).permitAll()

                // -- 역할 기반 접근 제어 ----------------------------------------
                // 사용자 관리 -- ADMIN 전용
                .pathMatchers("/api/users/**").hasRole("ADMIN")

                // 품목(Items) CUD -- ADMIN 전용 (조회는 모든 인증 사용자)
                .pathMatchers(HttpMethod.POST,   "/api/items/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.PUT,    "/api/items/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/items/**").hasRole("ADMIN")

                // 거래처(Clients) CUD -- ADMIN 또는 SALES
                .pathMatchers(HttpMethod.POST, "/api/clients/**").hasAnyRole("ADMIN", "SALES")
                .pathMatchers(HttpMethod.PUT,  "/api/clients/**").hasAnyRole("ADMIN", "SALES")

                // 생산지시서 -- ADMIN 또는 PRODUCTION
                .pathMatchers("/api/production-orders/**").hasAnyRole("ADMIN", "PRODUCTION")

                // 출하지시서/출하현황 -- ADMIN 또는 SHIPPING
                .pathMatchers("/api/shipment-orders/**", "/api/shipments/**")
                    .hasAnyRole("ADMIN", "SHIPPING")

                // 나머지 모든 요청 -- 인증 필요
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            );

        return http.build();
    }
}
