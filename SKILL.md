# SKILL.md - 프로젝트에 필요한 기술 및 지식

## 백엔드 스킬

### Spring Boot
- Spring Boot 4.0.3 사용 (Spring Framework 7.x, Gradle 8.14+ 필요)
- Auto Configuration, Profile 기반 설정 관리 (local/dev/prod)
- `application.yml`에서 증권사별 API URL/엔드포인트 외부화
- RestTemplate / WebClient를 통한 외부 API 호출

### Java 21
- Record 클래스 활용 (DTO — `ApiResponse<T>`, `ErrorResponse`)
  - record 필드명과 동일한 정적 메서드명 사용 불가 → `ok()`/`fail()`로 명명
- Sealed 클래스 / Pattern Matching
- Virtual Threads (Project Loom) 활용 가능

### Spring Data JPA
- Entity 설계 및 연관관계 매핑
- JpaRepository 기반 CRUD
- `@AttributeConverter`를 통한 암호화 필드 자동 변환

### Spring Security + JWT
- Stateless 세션 정책 (`SessionCreationPolicy.STATELESS`)
- `BCryptPasswordEncoder`로 비밀번호 해시 저장
- `JwtTokenProvider`: jjwt 라이브러리 기반 토큰 생성/검증
  - Access Token: 짧은 만료 (15~30분)
  - Refresh Token: 긴 만료 (7일), 토큰 갱신 API (`POST /api/v1/auth/refresh`)
- `JwtAuthenticationFilter`: `OncePerRequestFilter`, `Authorization: Bearer <token>` 헤더 파싱
- `SecurityFilterChain`: CORS 허용, CSRF 비활성화, JWT 필터 등록
- 서비스 레이어에서 계좌 소유권 검증 (`account.userId == currentUser.id`)

### 보안 / 암호화
- AES-256-GCM 인증 암호화 (app-key, secret-key 저장)
  - `AesEncryptionUtil` (@Component): SHA-256으로 키→32바이트 변환, 랜덤 12바이트 IV, IV+암호문을 Base64 인코딩
  - GCM은 변조 감지 기능 내장 (CBC 대비 보안 우위)
- 암호화 키는 환경변수 또는 `application.yml`의 `encryption.aes.secret-key`로 관리
- 화면 출력 시 마스킹 처리: `MaskingUtil.maskKey()` (앞 4자리 + `****`), `maskAccountNumber()` (앞4+뒤2, 중간 마스킹)
- 사용자 비밀번호: BCrypt 해시 (평문 저장 금지)

### OAuth2 / API 인증
- 한국투자증권: OAuth2 토큰 기반 (Access Token 발급/갱신)
- 키움: API Key + 토큰 인증
- LS증권: OAuth2 (`/oauth2/token` API로 접근토큰 발급)

### SSE (Server-Sent Events)
- `SseEmitter`를 통한 서버 → 클라이언트 단방향 실시간 시세 스트리밍
- JWT 인증: `EventSource`는 커스텀 헤더 불가 → 쿼리 파라미터로 토큰 전달 (`?token=xxx`)
  - 서버에서 쿼리 파라미터의 JWT를 검증하는 별도 로직 필요
- 클라이언트: 브라우저 `EventSource` API (추가 라이브러리 불필요)
- 연결 끊김 시 브라우저 자동 재연결 (별도 구현 불필요)
- `produces = MediaType.TEXT_EVENT_STREAM_VALUE`로 컨트롤러 매핑
- 증권사 API에서 시세 수신 → SseEmitter.send()로 클라이언트에 push

## 프론트엔드 스킬

### React + TypeScript
- Vite 기반 프로젝트 구성
- 함수형 컴포넌트 + Hooks (`useState`, `useEffect`, `useContext`, `useCallback`)
- Custom Hooks: `useAuth` (JWT 관리), `usePolling` (30초 자동 갱신), `useSSE` (실시간 시세)
- React Router v7: 라우팅, ProtectedRoute (비인증 시 리다이렉트)

### Axios
- JWT 인터셉터: 요청 시 `Authorization: Bearer <token>` 자동 첨부
- 응답 인터셉터: 401 시 Refresh Token으로 자동 갱신 후 재요청
- API 서비스 모듈화 (`services/api.ts`, `services/accountApi.ts` 등)

### 상태 관리
- Zustand로 전역 상태 관리 (`authStore.ts` — accessToken, refreshToken, isAuthenticated)
- localStorage와 자동 동기화 (`setTokens()`, `clearTokens()`)
- 로그인/비로그인 상태별 메뉴 분기 (ProtectedRoute → Layout 중첩 구조)

### SSE 클라이언트
- `EventSource` API로 실시간 시세 수신
- JWT 토큰을 쿼리 파라미터로 전달 (`?token=xxx`)
- `useSSE` Custom Hook으로 구독/해제 관리

## 인프라 스킬

### Docker
- Docker Compose로 PostgreSQL 컨테이너 관리
- Volume 마운트를 통한 데이터 영속화
- `.env` 파일로 DB 계정 정보 관리

### PostgreSQL
- 테이블 설계 및 인덱싱
- 암호화 데이터 저장 (TEXT 타입)
- timestamp 관리 (created_at, updated_at)

## 테스트 스킬

### TDD (Test-Driven Development)
- Red → Green → Refactor 사이클
- 테스트를 먼저 작성하고 구현 코드 작성

### JUnit 5
- `@Test`, `@ParameterizedTest`, `@Nested`
- `@DisplayName`으로 테스트 의도 명시

### Mockito
- `@Mock`, `@InjectMocks`
- `when(...).thenReturn(...)` 패턴
- 외부 API 호출 모킹

### WireMock
- 증권사 API 통합 테스트 시 Mock Server
- 요청/응답 매핑

### Spring Boot Test
- `@SpringBootTest` 통합 테스트
- `@WebMvcTest` 컨트롤러 단위 테스트
- `@DataJpaTest` 리포지토리 테스트
- TestContainers로 PostgreSQL 테스트 DB

## 증권사별 API 특이사항

### 한국투자증권 (KIS)
- 문서: https://apiportal.koreainvestment.com/apiservice-apiservice
- 인증: OAuth2 (appkey + appsecret → access_token)
- 모의투자/실전투자 도메인 분리
- 요청 헤더에 tr_id(거래ID), custtype(고객유형) 등 필수

### 키움
- 문서: https://openapi.kiwoom.com/guide/apiguide?dummyVal=0
- 인증 방식 문서 확인 필요
- REST API 기반

### LS증권
- 문서: https://openapi.ls-sec.co.kr/apiservice
- 인증: OAuth2 (`/oauth2/token` API로 접근토큰 발급, appkey + secretkey 사용)
- REST API 기반
