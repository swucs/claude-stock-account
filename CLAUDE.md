# CLAUDE.md - 다중 증권사 주식 계좌 조회 웹앱

## 프로젝트 개요
다중 증권사(한국투자증권, 키움, LS증권)의 주식 계좌를 통합 조회하는 웹 애플리케이션.

## 기술 스택

### Back-end (`backend/`)
- **Java**: 21 (Zulu OpenJDK 21)
- **Framework**: Spring Boot 4.0.3
- **Build Tool**: Gradle 8.14 (Kotlin DSL)
- **ORM**: Spring Data JPA
- **Database**: PostgreSQL 17 (Docker 컨테이너, 데이터 volume 마운트)
- **Lombok**: 사용
- **API 문서**: SpringDoc OpenAPI 2.8.6 (Swagger UI)
- **인증/보안**: Spring Security + JWT (Access Token / Refresh Token, jjwt 0.12.6)
- **암호화**: AES-256-GCM (AesEncryptionUtil)

### Front-end (`frontend/`)
- **Framework**: React (Vite 기반, TypeScript)
- **상태 관리**: Zustand
- **HTTP 클라이언트**: Axios (JWT 인터셉터 — 401 시 자동 토큰 갱신)
- **라우팅**: React Router v7

### Infrastructure
- **DB**: Docker Compose로 PostgreSQL 17 실행, Named volume `pgdata`로 데이터 영속화

## 지원 증권사 및 API
| 증권사 | API 문서 URL |
|--------|-------------|
| 한국투자증권 (KIS) | https://apiportal.koreainvestment.com/apiservice-apiservice |
| 키움 | https://openapi.kiwoom.com/guide/apiguide?dummyVal=0 |
| LS증권 | https://openapi.ls-sec.co.kr/apiservice?group_id=ffd2def7-a118-40f7-a0ab-cd4c6a538a90&api_id=33bd887a-6652-4209-88cd-5324bc7c5e36 |

## 핵심 기능
1. **사용자 인증**: 회원가입, 로그인/로그아웃 (JWT Access Token + Refresh Token)
2. **잔고 조회**: 계좌별 보유 종목, 수량, 평가금액 조회 (30초 주기 자동 갱신)
3. **실시간 시세**: SSE (Server-Sent Events) 기반 실시간 주가 정보
4. **자동 갱신**: 잔고 조회 화면에서 30초마다 데이터 자동 갱신 (React useEffect + Axios polling)

## 핵심 설계 원칙
- **사용자별 계좌 관리**: 로그인한 사용자만 본인 계좌 접근 가능 (Account에 user_id FK)
- 증권사 선택 → 계좌 선택 흐름의 UI
- 각 증권사마다 복수 계좌 지원
- 계좌별 app-key, secret-key 등 인증정보 별도 관리
- 인증정보는 DB에 **암호화** 저장, 화면 출력 시 **마스킹** 처리
- API URL 및 엔드포인트는 `application.yml`에서 관리
- 증권사별 OAuth2 또는 API Key 인증 방식 지원
- TDD 기반 개발

## 개발 순서
1. 한국투자증권 (KIS) — 먼저 완성
2. 키움
3. LS증권

## 코딩 컨벤션

### 프로젝트 구조 (모노레포)
```
stock-account/
├── backend/                 # Spring Boot 백엔드
│   ├── src/main/java/com/stock/account/
│   │   ├── config/          # SecurityConfig, SwaggerConfig
│   │   ├── common/          # dto, exception, util
│   │   ├── controller/      # REST 컨트롤러
│   │   ├── service/         # 비즈니스 로직
│   │   ├── repository/      # JPA 리포지토리
│   │   ├── domain/          # 엔티티
│   │   ├── dto/             # 요청/응답 DTO
│   │   └── security/        # JWT 필터 등
│   ├── src/main/resources/  # application.yml (local/dev/prod)
│   ├── build.gradle.kts
│   └── gradlew
├── frontend/                # Vite + React + TypeScript
│   ├── src/
│   │   ├── components/      # layout/, common/
│   │   ├── pages/           # 각 화면 컴포넌트
│   │   ├── services/        # api.ts (Axios 인스턴스)
│   │   ├── store/           # authStore.ts (Zustand)
│   │   └── types/           # 공통 타입 정의
│   ├── package.json
│   └── vite.config.ts       # /api → localhost:8080 프록시
├── docker-compose.yml       # PostgreSQL 17
└── CLAUDE.md
```

### Back-end
- 패키지 구조: `com.stock.account`
- 계층 구조: `controller` / `service` / `repository` / `domain` / `dto` / `config` / `common` / `security`
- 공통 응답: `ApiResponse<T>` (record) — `ApiResponse.ok(data)`, `ApiResponse.fail(code, message)`
- 예외 처리: `ErrorCode` enum + `BusinessException` + `GlobalExceptionHandler`
- 암호화: `AesEncryptionUtil` (AES-256-GCM, SHA-256으로 키 변환, 랜덤 IV)
- 마스킹: `MaskingUtil.maskKey()`, `MaskingUtil.maskAccountNumber()`
- 증권사별 구현은 Strategy 패턴 또는 별도 패키지로 분리
- 테스트: JUnit 5 + Mockito, 통합 테스트 시 @SpringBootTest, 테스트 DB는 H2 인메모리
- REST API 경로: `/api/v1/...`
- 설정 파일: `application.yml` (profile 분리: local, dev, prod)

### Front-end
- 디렉토리: `frontend/` (Vite + React + TypeScript)
- 컴포넌트 구조: `pages/` / `components/` / `hooks/` / `services/` / `store/` / `types/`
- API 호출: `services/api.ts` (Axios 인스턴스, JWT 인터셉터 — 401 시 자동 refreshToken 갱신)
- 인증 상태: `store/authStore.ts` (Zustand, localStorage 동기화)
- 라우팅: `ProtectedRoute` → `Layout` → 페이지 중첩 구조
- 테스트: Vitest + React Testing Library

## 보안 주의사항
- 사용자 비밀번호는 BCrypt로 해시 저장
- JWT Access Token: 짧은 만료 (15~30분), Authorization 헤더로 전송
- JWT Refresh Token: 긴 만료 (7일), HttpOnly 쿠키 또는 별도 저장
- app-key, secret-key 등 민감 정보는 절대 평문 저장 금지
- AES-256-GCM으로 암호화 후 DB 저장 (인증 암호화 — 변조 감지)
- 화면 노출 시 앞 4자리만 표시하고 나머지는 `****`로 마스킹
- `.env` 파일이나 credential 파일은 `.gitignore`에 반드시 포함
- 타인의 계좌에 접근 불가하도록 서비스 레이어에서 소유권 검증 필수
- CORS 설정: React 개발 서버 (localhost:5173) 허용

## 화면 메뉴 구성 (React SPA)
### 비로그인 상태
- 로그인 (`/login`)
- 회원가입 (`/signup`)

### 로그인 상태
| 메뉴 | 라우트 | 설명 |
|------|--------|------|
| 대시보드 | `/` | 전체 증권사 잔고 요약 |
| 잔고 조회 | `/balance` | 선택 계좌 보유종목 (30초 자동 갱신 ON/OFF) |
| 실시간 시세 | `/price` | 보유종목 실시간 시세 (SSE) |
| 계좌 관리 | `/accounts` | 계좌 등록/수정/삭제 |
| 내 정보 / 로그아웃 | 우측 상단 드롭다운 |

> React Router의 ProtectedRoute로 비인증 시 `/login`으로 리다이렉트
