# TASK.md - 작업 추적

## 현재 진행 중
- [x] Phase 2: 계좌 및 인증정보 관리 ✅ (2026-03-15 완료)

## Phase 0: 프로젝트 초기 설정 ✅ (2026-03-14 완료)
### Back-end (`backend/`)
- [x] Spring Boot 프로젝트 초기화 (build.gradle.kts Kotlin DSL, Gradle 8.14, Java 21)
- [x] Docker Compose 작성 (PostgreSQL 17 + named volume `pgdata`)
- [x] application.yml 작성 (local/dev/prod 프로파일, DB, JWT, AES, 증권사 API URL)
- [x] 공통 응답 DTO (ApiResponse record — ok()/fail(), ErrorResponse)
- [x] 공통 예외 처리 구현 (ErrorCode enum, BusinessException, GlobalExceptionHandler)
- [x] 암호화 유틸 구현 (AES-256-GCM + TDD 테스트 6개 통과)
- [x] 마스킹 유틸 구현 (MaskingUtil — maskKey, maskAccountNumber)
- [x] SecurityConfig (CORS localhost:5173, CSRF 비활성화, Stateless, BCrypt)
- [x] SwaggerConfig (OpenAPI 메타정보, Bearer JWT SecurityScheme)
### Front-end (`frontend/`)
- [x] Vite + React + TypeScript 프로젝트 생성
- [x] 의존성 설치 (axios, react-router-dom@7, zustand, vitest, testing-library)
- [x] vite.config.ts 프록시 설정 (/api → localhost:8080)
- [x] Axios 인스턴스 설정 (JWT 인터셉터, 401 시 refreshToken 자동 갱신)
- [x] Zustand 인증 스토어 (authStore.ts — localStorage 동기화)
- [x] React Router 설정 (ProtectedRoute + Layout 중첩 라우팅)
- [x] 공통 레이아웃 컴포넌트 (Header, Sidebar, Layout)
- [x] Placeholder 페이지 6개 (Login, Signup, Dashboard, Balance, Price, Accounts)

## Phase 1: 사용자 인증 (JWT 기반) ✅ (2026-03-15 완료)
### Back-end
- [x] User Entity 작성 (email, password, name)
- [x] UserRepository 작성
- [x] UserService 작성 (회원가입, 조회, 수정)
- [x] JwtTokenProvider 구현 (Access Token + Refresh Token 생성/검증)
- [x] JwtAuthenticationFilter 구현 (OncePerRequestFilter)
- [x] SecurityConfig 설정 (JWT 필터 등록, AuthenticationEntryPoint, Stateless, CORS, BCrypt)
- [x] AuthController 작성 (회원가입/로그인/토큰 갱신 API)
- [x] UserApiController 작성 (내 정보 조회/수정 API)
### Front-end
- [x] authService 구현 (login, signup, refresh, getMe, updateMe)
- [x] 로그인 페이지 (폼 + 에러 처리 + 로딩 상태)
- [x] 회원가입 페이지 (폼 + 비밀번호 확인 + 에러 처리)
- [x] authStore에 userName 추가 (Header에 사용자 이름 표시)
### 테스트
- [x] 단위 테스트 (UserService 11개, JwtTokenProvider 7개)
- [x] 통합 테스트 (회원가입/로그인/JWT 검증/토큰 갱신/내 정보 조회/수정 7개)

## Phase 2: 계좌 및 인증정보 관리 ✅ (2026-03-15 완료)
- [x] Account Entity + BrokerType Enum 작성 (user_id FK 포함)
- [x] AccountRepository 작성 (findByUserId 등)
- [x] AccountService 작성 (CRUD + 암호화/복호화 + 소유권 검증)
- [x] AccountDto / Request / Response 작성 (마스킹 포함)
- [x] AccountController 작성 (로그인 사용자 기준)
- [x] 계좌 관리 화면 (React)
- [x] 단위 테스트 (Service, 암호화, 마스킹, 소유권 검증)
- [x] 통합 테스트 (Controller, Repository)

## Phase 3: 한국투자증권 (KIS) ✅ (2026-03-15 완료)
- [x] KIS API 문서 분석 및 정리
- [x] BrokerApiClient 인터페이스 정의
- [x] KisApiClient 구현 (인증) — 실전투자 URL/tr_id 적용
- [x] KIS 토큰 DB 영속화 (BrokerToken 엔티티 + 2-Layer 캐시: 메모리+DB, AES-256-GCM 암호화 저장)
  - REQUIRES_NEW 전파로 readOnly 트랜잭션 충돌 해결
- [x] KisApiClient 구현 (잔고 조회 + 30초 자동 갱신 API)
- [x] KisApiClient 구현 (실시간 시세 - SSE / SseEmitter, 3초 간격 polling)
- [x] 조회 화면 구현 (React: 증권사 탭 필터 → 계좌 선택 → 잔고/시세 조회)
- [x] 잔고 조회 30초 자동 갱신 구현 (useEffect + Axios polling, ON/OFF 토글)
- [x] KIS 관련 테스트 작성 (KisApiClient 8개, BalanceService 4개, PriceService 5개)
- [x] Service 인터페이스/구현체 분리 (service/impl 패키지)

## Phase 4: 키움
- [ ] 키움 API 문서 분석 및 정리
- [ ] KiwoomApiClient 구현 (인증)
- [ ] KiwoomApiClient 구현 (잔고(30초 갱신)/실시간시세)
- [ ] 키움 관련 테스트 작성

## Phase 5: LS증권
- [ ] LS API 문서 분석 및 정리
- [ ] LsApiClient 구현 (인증)
- [ ] LsApiClient 구현 (잔고(30초 갱신)/실시간시세)
- [ ] LS 관련 테스트 작성

## Phase 6: 통합 및 마무리
- [ ] 통합 대시보드
- [ ] 에러 처리 / 로깅 강화
- [ ] 보안 점검
- [ ] 성능 최적화

## 완료된 작업
- [x] Phase 0: 프로젝트 초기 설정 (2026-03-14)
- [x] Phase 1: 사용자 인증 — JWT 기반 (2026-03-15)
- [x] Phase 2: 계좌 및 인증정보 관리 — CRUD + 암호화 + 마스킹 (2026-03-15)
- [x] Phase 3: 한국투자증권(KIS) 연동 — 인증/잔고/시세/SSE/테스트 (2026-03-15)
