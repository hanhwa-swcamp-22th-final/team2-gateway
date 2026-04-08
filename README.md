# team2-gateway

Spring Cloud Gateway 기반 API 게이트웨이.
모든 `/api/*` 요청의 **JWT 검증 → 권한 체크 → 헤더 스트리핑 → 라우팅**을 일괄 처리한다.

> **Note**: `auth-hardening` Phase 2 산출물.
> Phase 5 완료 후 Nginx는 정적 파일 서빙 + TLS 종료 전용으로 역할이 축소되고, `/api/*` 프록시는 이 게이트웨이가 전담한다.

---

## 아키텍처 개요

```
Client
  │ HTTPS
  ▼
Nginx (8001) ─── 정적 SPA 서빙
  │ /api/*  HTTP proxy
  ▼
team2-gateway (8010) ─── JWT 검증(JWKS/RS256) + 권한 체크 + 헤더 스트리핑
  │
  ├─ backend-auth     :8011
  ├─ backend-master   :8012
  ├─ backend-activity :8013
  └─ backend-documents:8014
```

### Defense-in-Depth
- **Gateway 레이어**: JWKS(RS256)로 JWT 검증, 역할 기반 접근 제어, X-User-* 헤더 스트리핑.
- **백엔드 레이어**: 각 서비스의 `JwtAuthFilter`가 독립적으로 JWT를 재검증.
- Authorization 헤더는 다운스트림에 **그대로 전파**되어 백엔드 검증에 활용된다.

---

## 라우트 매핑

| 경로 패턴 | 업스트림 | 포트 |
|---|---|---|
| `/api/auth/**`, `/api/users/**`, `/api/company/**`, `/api/departments/**`, `/api/positions/**`, `/.well-known/jwks.json` | backend-auth | 8011 |
| `/api/clients/**`, `/api/items/**`, `/api/buyers/**`, `/api/countries/**`, `/api/ports/**`, `/api/currencies/**`, `/api/incoterms/**`, `/api/payment-terms/**` | backend-master | 8012 |
| `/api/activities/**`, `/api/activity-packages/**`, `/api/email-logs/**`, `/api/contacts/**` | backend-activity | 8013 |
| `/api/purchase-orders/**`, `/api/proforma-invoices/**`, `/api/commercial-invoices/**`, `/api/packing-lists/**`, `/api/production-orders/**`, `/api/shipment-orders/**`, `/api/shipments/**`, `/api/collections/**`, `/api/approval-requests/**`, `/api/emails/**`, `/api/docs-revisions/**` | backend-documents | 8014 |

---

## 접근 제어 정책

| 경로 | 허용 역할 |
|---|---|
| `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/forgot-password`, `/.well-known/jwks.json`, `/actuator/health` | 공개 |
| `POST/PUT/DELETE /api/items/**` | ADMIN |
| `POST/PUT /api/clients/**` | ADMIN, SALES |
| `/api/users/**` | ADMIN |
| `/api/production-orders/**` | ADMIN, PRODUCTION |
| `/api/shipment-orders/**`, `/api/shipments/**` | ADMIN, SHIPPING |
| 그 외 모든 경로 | 인증된 모든 사용자 |

---

## 실행

```bash
# 로컬
./gradlew bootRun

# Docker
docker build -t team2-gateway .
docker run -p 8010:8010 team2-gateway

# 빌드 & 테스트
./gradlew build
```

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8010` | 게이트웨이 포트 |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | `http://backend-auth:8011/.well-known/jwks.json` | JWKS 엔드포인트 |

---

## Phase 로드맵

| Phase | 내용 | 상태 |
|---|---|---|
| 0 | 현황 감사 | 완료 |
| 1 | backend-auth RS256 + JWKS 전환 | 진행 중 |
| **2** | **team2-gateway 스켈레톤 (이 레포)** | **진행 중** |
| 3 | 백엔드 JwtAuthFilter 헤더 신뢰 분기 제거 | 대기 |
| 4 | 프론트엔드 RT HttpOnly 쿠키 전환 | 대기 |
| 5 | Nginx Gateway cutover | 대기 |
