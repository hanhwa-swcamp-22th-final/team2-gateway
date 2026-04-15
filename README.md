# team2-gateway

Spring Cloud Gateway 기반 API 게이트웨이.
모든 `/api/*` 요청의 **JWT 검증 → 권한 체크 → 헤더 스트리핑 → 라우팅**을 일괄 처리한다.

현재 운영 중인 cutover 완료 상태 — Nginx 는 정적 SPA 서빙 + TLS 종료 전용,
모든 `/api/*` 프록시는 게이트웨이가 전담.

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

> **2-tier 정책**: 게이트웨이는 path 레벨 거친 검증, 세부 액션은 컨트롤러 `@PreAuthorize`.

### Path 레벨 (Gateway)

| 경로 | 허용 역할 |
|---|---|
| `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/forgot-password`, `/.well-known/jwks.json`, `/actuator/health` | 공개 |
| `/api/**/internal/**` | denyAll (외부 차단, 서비스 간 X-Internal-Token 호출 전용) |
| `POST/PUT/DELETE /api/items/**` | ADMIN |
| `POST/PUT /api/clients/**` | ADMIN, SALES |
| `DELETE /api/clients/**` | ADMIN |
| `/api/users/**` | ADMIN |
| `/api/production-orders/**`, `/api/shipment-orders/**`, `/api/shipments/**` | 인증 사용자 (영업담당자 read 가능) |
| 그 외 모든 경로 | 인증된 모든 사용자 |

### 컨트롤러 @PreAuthorize (세부 액션)

| 엔드포인트 | 허용 역할 |
|---|---|
| `PUT /api/shipments/{id}` (출하완료 처리) | ADMIN, SHIPPING |
| `PUT /api/production-orders/{id}/complete` (생산완료 처리) | ADMIN, PRODUCTION |
| `POST /api/purchase-orders/{id}/generate-production-order` | ADMIN, PRODUCTION |
| 그 외 PI/PO/CI/PL CUD | ADMIN, SALES |
| 활동기록 / 패키지 / 컨택 / 메일 | ADMIN, SALES |

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
| 0 | 현황 감사 | ✅ 완료 |
| 1 | backend-auth RS256 + JWKS 전환 | ✅ 완료 |
| 2 | team2-gateway 스켈레톤 | ✅ 완료 |
| 3 | 백엔드 JwtAuthFilter 헤더 신뢰 분기 제거 | ✅ 완료 |
| 4 | 프론트엔드 RT HttpOnly 쿠키 전환 | ✅ 완료 |
| 5 | Nginx → Gateway cutover | ✅ 완료 |
| 6 | k8s 전환 + ArgoCD GitOps + Image Updater | ✅ 완료 |
