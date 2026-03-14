# TASK.md - 작업 추적

## 현재 진행 중
- [ ] Phase 0: 프로젝트 초기 설정

## Phase 0: 프로젝트 초기 설정
### Back-end
- [ ] Spring Boot 프로젝트 초기화 (build.gradle, 디렉토리 구조)
- [ ] Docker Compose 작성 (PostgreSQL 15+ / volume 마운트)
- [ ] application.yml 작성 (DB, JWT 설정, 증권사 API URL/엔드포인트)
- [ ] 암호화 유틸 구현 (AES-256 + 테스트)
- [ ] 공통 예외 처리 구현
- [ ] CORS 설정 (React 개발 서버 허용)
### Front-end
- [ ] Vite + React + TypeScript 프로젝트 생성 (frontend/)
- [ ] Axios 인스턴스 설정 (JWT 인터셉터, 토큰 갱신)
- [ ] React Router 설정 (ProtectedRoute 포함)
- [ ] 공통 레이아웃 컴포넌트 (Header, Sidebar, Layout)

## Phase 1: 사용자 인증 (JWT 기반)
### Back-end
- [ ] User Entity 작성 (email, password, name)
- [ ] UserRepository 작성
- [ ] UserService 작성 (회원가입, 조회, 수정)
- [ ] JwtTokenProvider 구현 (Access Token + Refresh Token 생성/검증)
- [ ] JwtAuthenticationFilter 구현 (OncePerRequestFilter)
- [ ] SecurityConfig 설정 (Stateless, CORS, BCrypt)
- [ ] AuthController 작성 (회원가입/로그인/토큰 갱신 API)
- [ ] UserApiController 작성 (내 정보 조회/수정 API)
### Front-end
- [ ] AuthContext / useAuth 훅 구현 (토큰 관리)
- [ ] 로그인 페이지 (React)
- [ ] 회원가입 페이지 (React)
### 테스트
- [ ] 단위 테스트 (UserService, JwtTokenProvider)
- [ ] 통합 테스트 (회원가입/로그인/JWT 검증/토큰 갱신)

## Phase 2: 계좌 및 인증정보 관리
- [ ] Account Entity + BrokerType Enum 작성 (user_id FK 포함)
- [ ] AccountRepository 작성 (findByUserId 등)
- [ ] AccountService 작성 (CRUD + 암호화/복호화 + 소유권 검증)
- [ ] AccountDto / Request / Response 작성 (마스킹 포함)
- [ ] AccountController 작성 (로그인 사용자 기준)
- [ ] 계좌 관리 화면 (React)
- [ ] 단위 테스트 (Service, 암호화, 마스킹, 소유권 검증)
- [ ] 통합 테스트 (Controller, Repository)

## Phase 3: 한국투자증권 (KIS)
- [ ] KIS API 문서 분석 및 정리
- [ ] BrokerApiClient 인터페이스 정의
- [ ] KisApiClient 구현 (인증)
- [ ] KisApiClient 구현 (잔고 조회 + 30초 자동 갱신 API)
- [ ] KisApiClient 구현 (거래내역 조회)
- [ ] KisApiClient 구현 (실시간 시세 - SSE / SseEmitter)
- [ ] 조회 화면 구현 (React: 증권사 선택 → 계좌 선택 → 조회)
- [ ] 잔고 조회 30초 자동 갱신 구현 (useEffect + Axios polling, ON/OFF 토글)
- [ ] KIS 관련 테스트 작성 (자동 갱신 포함)

## Phase 4: 키움
- [ ] 키움 API 문서 분석 및 정리
- [ ] KiwoomApiClient 구현 (인증)
- [ ] KiwoomApiClient 구현 (잔고(30초 갱신)/거래내역/실시간시세)
- [ ] 키움 관련 테스트 작성

## Phase 5: LS증권
- [ ] LS API 문서 분석 및 정리
- [ ] LsApiClient 구현 (인증)
- [ ] LsApiClient 구현 (잔고(30초 갱신)/거래내역/실시간시세)
- [ ] LS 관련 테스트 작성

## Phase 6: 통합 및 마무리
- [ ] 통합 대시보드
- [ ] 에러 처리 / 로깅 강화
- [ ] 보안 점검
- [ ] 성능 최적화

## 완료된 작업
_(없음)_
