# PLAN.md - 구현 계획

## Phase 0: 프로젝트 초기 설정 ✅ (2026-03-14 완료)
- [x] **Back-end**: Spring Boot 4.0.3 프로젝트 생성 (`backend/`, Gradle 8.14 Kotlin DSL, Java 21)
- [x] 의존성 설정 (Spring Web, JPA, PostgreSQL, Lombok, Spring Security, jjwt 0.12.6, springdoc 2.8.6)
- [x] Docker Compose 파일 작성 (PostgreSQL 17 + named volume `pgdata`)
- [x] `application.yml` 기본 구성 (DB 연결, JWT 설정, 증권사 API URL/엔드포인트, AES 키)
- [x] 공통 패키지 구조 생성 (`config`, `common/{dto,exception,util}`, `controller`, `service`, `repository`, `domain`, `dto`, `security`)
- [x] 암호화 유틸 클래스 구현 (AES-256-GCM + TDD 테스트 6개)
- [x] 마스킹 유틸 구현 (`MaskingUtil.maskKey()`, `maskAccountNumber()`)
- [x] 공통 응답 DTO (`ApiResponse<T>` record — `ok()`/`fail()` 팩토리 메서드)
- [x] 공통 예외 처리 (`ErrorCode` enum, `BusinessException`, `GlobalExceptionHandler`)
- [x] SpringDoc OpenAPI (Swagger UI) 설정 (`SwaggerConfig` — Bearer JWT SecurityScheme)
- [x] Spring Security 설정 (`SecurityConfig` — CORS, CSRF 비활성화, Stateless, BCrypt)
- [x] **Front-end**: Vite + React + TypeScript 프로젝트 생성 (`frontend/`)
- [x] Axios 인스턴스 설정 (JWT 인터셉터, 401 시 refreshToken 자동 갱신)
- [x] Zustand 인증 스토어 (`authStore.ts` — localStorage 동기화)
- [x] React Router 설정 (ProtectedRoute + Layout 중첩 라우팅)
- [x] 공통 레이아웃 컴포넌트 (Header, Sidebar, Layout)
- [x] Placeholder 페이지 6개 (Login, Signup, Dashboard, Balance, Price, Accounts)

> **참고**: Java 25가 아닌 Java 21 사용 (로컬 환경). Spring Boot 4.0.3이 Gradle 8.14+를 요구하여 8.12→8.14로 변경.

## Phase 1: 사용자 인증 (JWT 기반) ✅ (2026-03-15 완료)
- [x] **Entity 설계**: `User` (이메일(unique), 비밀번호(BCrypt), 이름, 생성일, 수정일)
- [x] **Repository**: `UserRepository` (JPA)
- [x] **Service**: `UserService` — 회원가입, 로그인, 토큰 갱신, 사용자 조회, 정보 수정
- [x] **JWT 구현**: `JwtTokenProvider`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`
- [x] **DTO**: `SignupRequest`, `LoginRequest`, `TokenResponse`, `UserResponse`, `UserUpdateRequest`, `RefreshTokenRequest`
- [x] **Controller**: `AuthController`, `UserApiController`
- [x] **React 화면**: 로그인/회원가입 폼 구현, authService, Header 사용자 이름 표시
- [x] **테스트**: JwtTokenProvider 단위 7개, UserService 단위 11개, 통합 테스트 7개 (총 25개 통과)

## Phase 2: 계좌 및 인증정보 관리 ✅ (2026-03-15 완료)
- [x] **Entity 설계**
  - `BrokerType` (enum): KIS, KIWOOM, LS
  - `Account`: 증권사, 계좌번호, 계좌명, 인증정보(암호화), **user_id (FK → User)**
- [x] **Repository**: `AccountRepository` (JPA) — `findByUserId()` 등 사용자 기반 조회
- [x] **Service**: `AccountService` — CRUD + 암호화/복호화 + **소유권 검증**
- [x] **DTO**: `AccountDto`, `AccountCreateRequest`, `AccountResponse` (마스킹 적용)
- [x] **Controller**: `AccountController` — 계좌 등록/수정/삭제/목록 (로그인 사용자 기준)
- [x] **React 화면**: 계좌 관리 페이지 (등록 폼, 목록, 수정/삭제)
- [x] **테스트**: 단위 테스트 + 통합 테스트 (TDD, 소유권 검증 포함)

## Phase 3: 한국투자증권 (KIS) 연동 ✅ (2026-03-15 완료)
> 상세 API 스펙: [BROKER-API.md](BROKER-API.md) 섹션 1 참조
- [x] **인증**: `POST /oauth2/tokenP` — OAuth2 토큰 발급/갱신 (24시간 유효)
- [x] **토큰 영속화**: BrokerToken 엔티티 + 2-Layer 캐시(메모리+DB) — 서버 재시작 시 토큰 재사용 (KIS 하루 1회 발급 권고 대응)
  - `@Transactional(propagation = REQUIRES_NEW)` — readOnly 트랜잭션 충돌 해결
- [x] **잔고 조회**: `GET /uapi/.../inquire-balance` (tr_id: `TTTC8434R`, 실전투자) → 응답 매핑 → 화면 (30초 자동 갱신)
- [x] **실시간 시세**: `GET /uapi/.../inquire-price` (tr_id: `FHKST01010100`) → SseEmitter 스트리밍 (3초 간격 polling)
- [x] **React 화면**: 증권사 탭 필터 → 계좌 선택 → 잔고 조회 / 실시간 시세 (탭 UI 통일)
- [x] **자동 갱신**: 잔고 조회 30초 polling (useEffect + Axios, 갱신 ON/OFF 토글)
- [x] **테스트**: KisApiClient 8개, BalanceService 4개, PriceService 5개 단위 테스트

## Phase 4: 키움 연동 ✅ (2026-03-15 완료)
> 상세 API 스펙: [BROKER-API.md](BROKER-API.md) 섹션 2 참조
- [x] **인증**: `POST /oauth2/token` — OAuth2 토큰 발급 (application/x-www-form-urlencoded)
- [x] **토큰 영속화**: KiwoomTokenCache (2-Layer 캐시: 메모리+DB, BrokerType.KIWOOM)
- [x] **잔고 조회**: `GET /api/dostk/acnt` (tr_id: `TTTC8434R`) → 응답 매핑
- [x] **실시간 시세**: `GET /api/dostk/mrkt` (tr_id: `FHKST01010100`) → getCurrentPrice()
- [x] 테스트 작성 (KiwoomApiClientTest 11개)

## Phase 5: LS증권 연동
> 상세 API 스펙: [BROKER-API.md](BROKER-API.md) 섹션 3 참조
- [ ] **인증**: `POST /oauth2/token` — OAuth2 토큰 발급 (appkey + appsecretkey + scope)
- [ ] **잔고 조회**: `POST /stock/accno` (tr_cd: `t0424`) → 응답 매핑
- [ ] **실시간 시세**: `POST /stock/market-data` (tr_cd: `t1102`) → SseEmitter 스트리밍
- [ ] 테스트 작성

## Phase 6: 통합 및 마무리
- [ ] 증권사 통합 대시보드 (전 증권사 잔고 합산 등)
- [ ] 에러 처리 및 로깅 강화
- [ ] 보안 점검 (암호화, 마스킹, XSS 방지 등)
- [ ] 성능 최적화 및 캐싱
- [ ] README 작성

---

## 아키텍처 개요

```
[React SPA (Vite)]                [Spring Boot API Server]
   │                                      │
   │  Axios + JWT (Bearer Token)          │
   ├──────── /api/v1/... ──────▶ [JwtAuthenticationFilter]
   │  ▲                                   │
   │  │ 30초 polling (Axios)        (JWT 토큰 검증)
   │  │ SSE (EventSource + token)         │
   │  └──────────────────────────── [Controller]
                                          │
                                          ▼
                                    [Service Layer]
                                 (소유권 검증: user_id)
                                  /       │        \
                           [KIS API]  [키움 API]  [LS API]
                                          │
                                          ▼
                                   [JPA Repository]
                                          │
                                          ▼
                                 [PostgreSQL (Docker)]
                                 ┌─────────┴─────────┐
                                 │  users  │ account  │
                                 │         │ (user_id)│
                                 └─────────┴──────────┘
```

## 증권사 API 추상화 설계

```java
// 공통 인터페이스
public interface BrokerApiClient {
    TokenResponse authenticate(Account account);
    BalanceResponse getBalance(Account account, String token);
    void streamRealTimePrice(Account account, String token, List<String> stockCodes, SseEmitter emitter);
}

// 증권사별 구현
KisApiClient implements BrokerApiClient
KiwoomApiClient implements BrokerApiClient
LsApiClient implements BrokerApiClient
```

## DB 스키마 (초안)

```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,           -- BCrypt 해시
    name        VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP DEFAULT now(),
    updated_at  TIMESTAMP DEFAULT now()
);

CREATE TABLE account (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    broker_type     VARCHAR(20) NOT NULL,       -- KIS, KIWOOM, LS
    account_number  VARCHAR(50) NOT NULL,
    account_name    VARCHAR(100),
    app_key         TEXT NOT NULL,              -- AES-256 암호화
    secret_key      TEXT NOT NULL,              -- AES-256 암호화
    additional_info TEXT,                        -- 증권사별 추가 인증정보 (JSON, 암호화)
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_account_user_id ON account(user_id);

CREATE TABLE broker_token (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    broker_type     VARCHAR(20) NOT NULL,       -- KIS, KIWOOM, LS
    access_token    TEXT NOT NULL,              -- AES-256-GCM 암호화
    token_type      VARCHAR(20) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now(),
    UNIQUE (account_id, broker_type)
);
```
