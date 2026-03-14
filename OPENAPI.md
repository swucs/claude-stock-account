# OPENAPI.md - Back-end REST API 명세

> SpringDoc OpenAPI (Swagger UI) 기반 — 접속 경로: `http://localhost:8080/swagger-ui.html`

## 공통 사항

### Base URL
```
/api/v1
```

### 공통 응답 형식
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ACCOUNT_NOT_FOUND",
    "message": "계좌를 찾을 수 없습니다."
  }
}
```

### 공통 에러 코드
| HTTP Status | 코드 | 설명 |
|-------------|------|------|
| 400 | `INVALID_REQUEST` | 잘못된 요청 파라미터 |
| 401 | `UNAUTHORIZED` | 로그인 필요 |
| 403 | `FORBIDDEN` | 해당 리소스에 대한 접근 권한 없음 (타인의 계좌 등) |
| 404 | `ACCOUNT_NOT_FOUND` | 계좌를 찾을 수 없음 |
| 409 | `DUPLICATE_EMAIL` | 이미 가입된 이메일 |
| 404 | `BROKER_NOT_FOUND` | 지원하지 않는 증권사 |
| 500 | `BROKER_API_ERROR` | 증권사 API 호출 실패 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

---

## 0. 인증 API (JWT)

> 모든 인증이 필요한 API는 `Authorization: Bearer <access_token>` 헤더 필수

### 0.1 회원가입
```
POST /api/v1/auth/signup
```
**Request Body**
```json
{
  "email": "user@example.com",
  "password": "securePassword123!",
  "name": "홍길동"
}
```
**Response 201**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "createdAt": "2026-03-14T10:00:00"
  }
}
```
**Error 409** — 이메일 중복 시

### 0.2 로그인
```
POST /api/v1/auth/login
```
**Request Body**
```json
{
  "email": "user@example.com",
  "password": "securePassword123!"
}
```
**Response 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```
**Error 401** — 이메일 또는 비밀번호 불일치

### 0.3 토큰 갱신
```
POST /api/v1/auth/refresh
```
**Request Body**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```
**Response 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...(새 토큰)",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...(새 토큰)",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

**Error 401** — Refresh Token 만료 또는 유효하지 않음

### 0.4 내 정보 조회
```
GET /api/v1/users/me
```
> `Authorization: Bearer <token>` 필수

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "createdAt": "2026-03-14T10:00:00"
  }
}
```

### 0.5 내 정보 수정
```
PUT /api/v1/users/me
```
**Request Body**
```json
{
  "name": "김철수",
  "currentPassword": "oldPassword123!",
  "newPassword": "newPassword456!"
}
```
> name만 변경 시 password 필드 생략 가능. 비밀번호 변경 시 currentPassword 필수.

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "김철수",
    "createdAt": "2026-03-14T10:00:00"
  }
}
```

---

## 1. 계좌 관리 API

> 모든 계좌 API는 **JWT 인증 필수** (`Authorization: Bearer <token>`)이며, 토큰에서 추출한 사용자의 계좌만 조회/관리 가능

### 1.1 계좌 목록 조회
```
GET /api/v1/accounts
```
**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| brokerType | String | N | 증권사 필터 (KIS, KIWOOM, LS) |

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "brokerType": "KIS",
      "accountNumber": "5012****01",
      "accountName": "한투 주식계좌",
      "appKey": "PSxq****",
      "secretKey": "HgT2****",
      "createdAt": "2026-03-14T10:00:00",
      "updatedAt": "2026-03-14T10:00:00"
    }
  ]
}
```
> appKey, secretKey는 앞 4자리만 노출, 나머지 마스킹 처리

### 1.2 계좌 상세 조회
```
GET /api/v1/accounts/{accountId}
```
**Path Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| accountId | Long | 계좌 ID |

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "brokerType": "KIS",
    "accountNumber": "5012****01",
    "accountName": "한투 주식계좌",
    "appKey": "PSxq****",
    "secretKey": "HgT2****",
    "createdAt": "2026-03-14T10:00:00",
    "updatedAt": "2026-03-14T10:00:00"
  }
}
```

### 1.3 계좌 등록
```
POST /api/v1/accounts
```
**Request Body**
```json
{
  "brokerType": "KIS",
  "accountNumber": "50123456-01",
  "accountName": "한투 주식계좌",
  "appKey": "PSxqDVE1...",
  "secretKey": "HgT2nVa9...",
  "additionalInfo": {
    "cano": "50123456",
    "acnt_prdt_cd": "01"
  }
}
```
**Response 201**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "brokerType": "KIS",
    "accountNumber": "5012****01",
    "accountName": "한투 주식계좌"
  }
}
```

### 1.4 계좌 수정
```
PUT /api/v1/accounts/{accountId}
```
**Request Body** — 1.3과 동일 (변경할 필드만 포함 가능)

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "brokerType": "KIS",
    "accountNumber": "5012****01",
    "accountName": "한투 주식계좌(수정)",
    "appKey": "PSxq****",
    "secretKey": "HgT2****",
    "createdAt": "2026-03-14T10:00:00",
    "updatedAt": "2026-03-14T11:30:00"
  }
}
```

### 1.5 계좌 삭제
```
DELETE /api/v1/accounts/{accountId}
```
**Response 204** — No Content

---

## 2. 증권사 조회 API

### 2.1 지원 증권사 목록
```
GET /api/v1/brokers
```
**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "code": "KIS",
      "name": "한국투자증권",
      "authType": "OAUTH2"
    },
    {
      "code": "KIWOOM",
      "name": "키움증권",
      "authType": "API_KEY"
    },
    {
      "code": "LS",
      "name": "LS증권",
      "authType": "OAUTH2 (/oauth2/token)"
    }
  ]
}
```

### 2.2 증권사별 계좌 목록
```
GET /api/v1/brokers/{brokerType}/accounts
```
**Path Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| brokerType | String | 증권사 코드 (KIS, KIWOOM, LS) |

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "brokerType": "KIS",
      "accountNumber": "5012****01",
      "accountName": "한투 주식계좌",
      "appKey": "PSxq****",
      "secretKey": "HgT2****",
      "createdAt": "2026-03-14T10:00:00",
      "updatedAt": "2026-03-14T10:00:00"
    },
    {
      "id": 3,
      "brokerType": "KIS",
      "accountNumber": "5098****02",
      "accountName": "한투 연금계좌",
      "appKey": "Abcd****",
      "secretKey": "Efgh****",
      "createdAt": "2026-03-14T12:00:00",
      "updatedAt": "2026-03-14T12:00:00"
    }
  ]
}
```

---

## 3. 잔고 조회 API

### 3.1 계좌 잔고 조회
```
GET /api/v1/accounts/{accountId}/balance
```
> 30초 자동 갱신 시 이 API를 Axios로 polling (Authorization 헤더에 JWT 포함)

**Path Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| accountId | Long | 계좌 ID |

**Response 200**
```json
{
  "success": true,
  "data": {
    "accountId": 1,
    "brokerType": "KIS",
    "totalEvaluation": 15230000,
    "totalPurchase": 14500000,
    "totalProfitLoss": 730000,
    "totalProfitRate": 5.03,
    "holdings": [
      {
        "stockCode": "005930",
        "stockName": "삼성전자",
        "quantity": 100,
        "avgPurchasePrice": 72000,
        "currentPrice": 75500,
        "evaluation": 7550000,
        "profitLoss": 350000,
        "profitRate": 4.86
      },
      {
        "stockCode": "000660",
        "stockName": "SK하이닉스",
        "quantity": 50,
        "avgPurchasePrice": 153600,
        "currentPrice": 153600,
        "evaluation": 7680000,
        "profitLoss": 380000,
        "profitRate": 5.21
      }
    ],
    "retrievedAt": "2026-03-14T10:30:00"
  }
}
```

---

## 4. 거래내역 조회 API

### 4.1 거래내역 조회
```
GET /api/v1/accounts/{accountId}/transactions
```
**Path Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| accountId | Long | 계좌 ID |

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | String | Y | 조회 시작일 (yyyy-MM-dd) |
| endDate | String | Y | 조회 종료일 (yyyy-MM-dd) |
| tradeType | String | N | 거래 유형 (BUY, SELL, ALL). 기본값: ALL |
| page | Integer | N | 페이지 번호. 기본값: 0 |
| size | Integer | N | 페이지 크기. 기본값: 20 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "transactionDate": "2026-03-13",
        "transactionTime": "09:31:25",
        "stockCode": "005930",
        "stockName": "삼성전자",
        "tradeType": "BUY",
        "quantity": 50,
        "price": 72000,
        "totalAmount": 3600000,
        "fee": 3600,
        "tax": 0
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```

---

## 5. 실시간 시세 API

### 5.1 실시간 시세 스트리밍 (SSE)
```
GET /api/v1/stocks/price/stream
```
> Server-Sent Events (SSE) — `Content-Type: text/event-stream`
> 브라우저 `EventSource` API로 연결, 자동 재연결 지원

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| stockCodes | String | Y | 종목 코드 (쉼표 구분, e.g. `005930,000660`) |
| accountId | Long | Y | 인증에 사용할 계좌 ID |
| token | String | Y | JWT Access Token (EventSource는 커스텀 헤더 불가하므로 쿼리로 전달) |

**SSE Event Stream (Server → Client)**
```
event: price
data: {"stockCode":"005930","stockName":"삼성전자","currentPrice":75500,"changePrice":1500,"changeRate":2.03,"volume":12345678,"high":76000,"low":74000,"open":74500,"timestamp":"2026-03-14T10:30:15"}

event: price
data: {"stockCode":"000660","stockName":"SK하이닉스","currentPrice":153600,"changePrice":2100,"changeRate":1.39,"volume":5432100,"high":155000,"low":151000,"open":152000,"timestamp":"2026-03-14T10:30:16"}
```

**클라이언트 사용 예시 (React + EventSource)**
```typescript
// useSSE Custom Hook
const token = useAuth().accessToken;
const source = new EventSource(
  `/api/v1/stocks/price/stream?stockCodes=005930,000660&accountId=1&token=${token}`
);
source.addEventListener('price', (event: MessageEvent) => {
  const price = JSON.parse(event.data);
  // setState로 UI 업데이트
});
source.onerror = () => {
  // 브라우저가 자동 재연결 시도
};
// 컴포넌트 unmount 시
return () => source.close();
```

### 5.2 현재가 단건 조회 (REST)
```
GET /api/v1/stocks/{stockCode}/price
```
**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| brokerType | String | Y | 시세 조회할 증권사 |
| accountId | Long | Y | 인증에 사용할 계좌 ID |

**Response 200**
```json
{
  "success": true,
  "data": {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "currentPrice": 75500,
    "changePrice": 1500,
    "changeRate": 2.03,
    "volume": 12345678,
    "high": 76000,
    "low": 74000,
    "open": 74500,
    "timestamp": "2026-03-14T10:30:15"
  }
}
```

---

## SpringDoc 설정

### 의존성 (build.gradle)
```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6'
```

### application.yml
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
```

### API 그룹 구성
| 태그 | 설명 |
|------|------|
| Auth | 사용자 인증 (JWT 로그인, 회원가입, 토큰 갱신, 내 정보) |
| Account | 계좌 관리 (CRUD, 로그인 필수) |
| Broker | 증권사 정보 |
| Balance | 잔고 조회 (30초 polling, 로그인 필수) |
| Transaction | 거래내역 조회 (로그인 필수) |
| Price | 시세 조회 (REST + SSE, 로그인 필수) |
