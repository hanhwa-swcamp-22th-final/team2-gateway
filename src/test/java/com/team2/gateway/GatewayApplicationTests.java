package com.team2.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 애플리케이션 컨텍스트 로드 스모크 테스트.
 *
 * ReactiveJwtDecoder 는 시작 시 JWKS URI 에 접근하므로
 * 테스트 환경에서는 MockitoBean 으로 대체한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTests {

    @MockitoBean
    ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void contextLoads() {
        // Spring 컨텍스트가 정상적으로 로드되면 통과
    }
}
