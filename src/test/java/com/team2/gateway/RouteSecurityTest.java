package com.team2.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 라우트 보안 기본 검증.
 *
 * 다운스트림 서비스가 없으므로 실제 프록시 응답 대신
 * 인증/인가 레이어(401/403)만 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RouteSecurityTest {

    @MockBean
    ReactiveJwtDecoder reactiveJwtDecoder;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void actuatorHealth_shouldBeAccessibleWithoutAuth() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedRoute_withoutToken_shouldReturn401() {
        // 토큰 없이 보호된 경로 접근 -> 401 Unauthorized
        webTestClient.get()
                .uri("/api/proforma-invoices")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
