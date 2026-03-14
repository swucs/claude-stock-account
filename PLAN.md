# PLAN.md - 구현 계획

## Phase 0: 프로젝트 초기 설정
- [ ] **Back-end**: Spring Boot 4.0.3 프로젝트 생성 (Gradle, Java 25+)
- [ ] 의존성 설정 (Spring Web, JPA, PostgreSQL, Lombok, Spring Security, jjwt 등)
- [ ] Docker Compose 파일 작성 (PostgreSQL + volume)
- [ ] `application.yml` 기본 구성 (DB 연결, JWT 설정, 증권사 API URL/엔드포인트)
- [ ] 공통 패키지 구조 생성
- [ ] 암호화 유틸 클래스 구현 (AES-256)
- [ ] 공통 예외 처리 (GlobalExceptionHandler)
- [ ] SpringDoc OpenAPI (Swagger UI) 설정
- [ ] CORS 설정 (React 개발 서버 허용)
- [ ] **Front-end**: Vite + React + TypeScript 프로젝트 생성 (`frontend/`)
- [ ] Axios 인스턴스 설정 (JWT 인터셉터, 토큰 갱신 로직)
- [ ] React Router 설정 (ProtectedRoute 포함)
- [ ] 공통 레이아웃 컴포넌트 (Header, Sidebar, Layout)

## Phase 1: 사용자 인증 (JWT 기반)
- [ ] **Entity 설계**
  - `User`: 이메일(unique), 비밀번호(BCrypt), 이름, 생성일, 수정일
- [ ] **Repository**: `UserRepository` (JPA)
- [ ] **Service**: `UserService` — 회원가입, 사용자 조회, 정보 수정
- [ ] **JWT 구현**
  - `JwtTokenProvider`: 토큰 생성/검증/파싱 (Access Token + Refresh Token)
  - `JwtAuthenticationFilter`: OncePerRequestFilter, Authorization 헤더에서 토큰 추출
  - `SecurityConfig`: Stateless 세션, CORS 설정, BCrypt PasswordEncoder
- [ ] **DTO**: `SignupRequest`, `LoginRequest`, `TokenResponse`, `UserResponse`
- [ ] **Controller**: `AuthController` — 회원가입/로그인/토큰 갱신 API, `UserApiController` — 내 정보 API
- [ ] **React 화면**: 로그인 페이지, 회원가입 페이지, AuthContext (토큰 관리)
- [ ] **테스트**: 회원가입/로그인/JWT 검증/토큰 갱신 단위 + 통합 테스트 (TDD)

## Phase 2: 계좌 및 인증정보 관리
- [ ] **Entity 설계**
  - `BrokerType` (enum): KIS, KIWOOM, LS
  - `Account`: 증권사, 계좌번호, 계좌명, 인증정보(암호화), **user_id (FK → User)**
- [ ] **Repository**: `AccountRepository` (JPA) — `findByUserId()` 등 사용자 기반 조회
- [ ] **Service**: `AccountService` — CRUD + 암호화/복호화 + **소유권 검증**
- [ ] **DTO**: `AccountDto`, `AccountCreateRequest`, `AccountResponse` (마스킹 적용)
- [ ] **Controller**: `AccountController` — 계좌 등록/수정/삭제/목록 (로그인 사용자 기준)
- [ ] **React 화면**: 계좌 관리 페이지 (등록 폼, 목록, 수정/삭제)
- [ ] **테스트**: 단위 테스트 + 통합 테스트 (TDD, 소유권 검증 포함)

## Phase 3: 한국투자증권 (KIS) 연동
> 상세 API 스펙: [BROKER-API.md](BROKER-API.md) 섹션 1 참조
- [ ] **인증**: `POST /oauth2/tokenP` — OAuth2 토큰 발급/갱신 (24시간 유효)
- [ ] **잔고 조회**: `GET /uapi/.../inquire-balance` (tr_id: `TTTC8434R`) → 응답 매핑 → 화면 (30초 자동 갱신)
- [ ] **실시간 시세**: `GET /uapi/.../inquire-price` (tr_id: `FHKST01010100`) → SseEmitter 스트리밍
- [ ] **React 화면**: 증권사 선택 → 계좌 선택 → 기능별 탭 (잔고/시세)
- [ ] **자동 갱신**: 잔고 조회 30초 polling (useEffect + Axios, 갱신 ON/OFF 토글)
- [ ] **테스트**: Mock API 기반 단위 테스트, WireMock 통합 테스트

## Phase 4: 키움 연동
> 상세 API 스펙: [BROKER-API.md](BROKER-API.md) 섹션 2 참조
- [ ] **인증**: `POST /oauth2/token` — OAuth2 토큰 발급
- [ ] **잔고 조회**: `GET /api/dostk/acnt` (tr_id: `TTTC8434R`) → 응답 매핑
- [ ] **실시간 시세**: `GET /api/dostk/mrkt` (tr_id: `FHKST01010100`) → SseEmitter 스트리밍
- [ ] 테스트 작성

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
```
