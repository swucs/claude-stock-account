# BROKER-API.md - 증권사별 API 호출 계획

> 아래 정보는 2025년 기준 공개 정보 기반이며, 실제 구현 전 공식 문서에서 최신 스펙을 반드시 재확인할 것.

---

## 1. 한국투자증권 (KIS)

### Base URL
| 환경 | URL |
|------|-----|
| 실전투자 | `https://openapi.koreainvestment.com:9443` |

> 모의투자 계좌는 사용하지 않음. 실전투자 URL만 사용한다.

### 공통 요청 헤더
| 헤더 | 값 |
|------|-----|
| `Content-Type` | `application/json; charset=utf-8` |
| `authorization` | `Bearer {access_token}` |
| `appkey` | 앱 키 |
| `appsecret` | 앱 시크릿 |
| `tr_id` | 각 API별 트랜잭션 ID |
| `custtype` | `"P"` (개인) |

### 1-1. 인증 (접근토큰 발급)
| 항목 | 내용 |
|------|------|
| **URL** | `POST /oauth2/tokenP` |
| **Content-Type** | `application/json` |

**Request Body**
```json
{
  "grant_type": "client_credentials",
  "appkey": "{앱키}",
  "appsecret": "{앱시크릿}"
}
```

**Response**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9...",  // 접근토큰 → accessToken
  "token_type": "Bearer",                                       // 토큰타입 → tokenType
  "expires_in": 86400,                                           // 만료시간(초) → expiresIn
  "access_token_token_expired": "2026-03-15 21:25:00"            // 토큰만료일시 → accessTokenExpired
}
```
> 토큰 발급은 1분에 1회 제한. 24시간 유효.

### 1-2. 잔고 조회
| 항목 | 내용 |
|------|------|
| **URL** | `GET /uapi/domestic-stock/v1/trading/inquire-balance` |
| **tr_id** | `TTTC8434R` (실전투자) |

**Request Headers**

| 헤더 | 값 | 설명 |
|------|-----|------|
| `Content-Type` | `application/json; charset=utf-8` | 컨텐츠 타입 |
| `authorization` | `Bearer {access_token}` | 접근토큰 |
| `appkey` | `{앱키}` | 앱 키 |
| `appsecret` | `{앱시크릿}` | 앱 시크릿 |
| `tr_id` | `TTTC8434R` | 트랜잭션ID (실전투자) |
| `tr_cont` | `""` (최초) / `"N"` (연속) | 연속거래 여부 |
| `custtype` | `"P"` | 고객타입 (P: 개인) |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `CANO` | String | Y | 계좌번호 앞 8자리 | — |
| `ACNT_PRDT_CD` | String | Y | 계좌상품코드 뒤 2자리 | — |
| `AFHR_FLPR_YN` | String | N | 시간외단일가여부 | `"N"` |
| `INQR_DVSN` | String | N | 조회구분 (`"01"`:대출일별, `"02"`:종목별) | `"02"` |
| `UNPR_DVSN` | String | N | 단가구분 (`"01"`) | `"01"` |
| `FUND_STTL_ICLD_YN` | String | N | 펀드결제분포함여부 | `"N"` |
| `FNCG_AMT_AUTO_RDPT_YN` | String | N | 융자금액자동상환여부 | `"N"` |
| `PRCS_DVSN` | String | N | 처리구분 (`"00"`:전일매매포함) | `"00"` |
| `CTX_AREA_FK100` | String | N | 연속조회검색조건 | `""` |
| `CTX_AREA_NK100` | String | N | 연속조회키 | `""` |

**Response**
```json
{
  "output1": [
    {
      "pdno": "005930",                // 종목코드 → stockCode
      "prdt_name": "삼성전자",          // 종목명 → stockName
      "hldg_qty": "100",              // 보유수량 → quantity
      "pchs_avg_pric": "72000.00",    // 매입평균가격 → avgPurchasePrice
      "prpr": "75500",                // 현재가 → currentPrice
      "evlu_amt": "7550000",          // 평가금액 → evaluation
      "evlu_pfls_amt": "350000",      // 평가손익금액 → profitLoss
      "evlu_pfls_rt": "4.86"          // 평가손익율 → profitRate
    }
  ],
  "output2": [
    {
      "tot_evlu_amt": "15230000",      // 총평가금액 → totalEvaluation
      "pchs_amt_smtl_amt": "14500000", // 매입금액합계 → totalPurchase
      "evlu_pfls_smtl_amt": "730000"   // 평가손익합계 → totalProfitLoss
    }
  ]
}
```

**Response Headers**

| 헤더 | 값 | 설명 |
|------|-----|------|
| `tr_cont` | `"F"` 또는 `"M"` | 다음 데이터 있음 (연속조회 필요) |
| `tr_cont` | `"D"` 또는 `"E"` | 마지막 데이터 (연속조회 종료) |

**연속조회 흐름**
> 보유종목이 많을 경우 한 번의 호출로 전체 데이터를 받지 못할 수 있다.
> 응답 헤더 `tr_cont`가 `"F"` 또는 `"M"`이면 다음 데이터가 존재하므로, 응답 Body의 `CTX_AREA_FK100`, `CTX_AREA_NK100` 값을 다음 요청의 Query Parameter에 그대로 넘기고 요청 헤더 `tr_cont`를 `"N"`으로 설정하여 반복 호출한다.
> `tr_cont`가 `"D"` 또는 `"E"`이면 마지막 데이터이므로 호출을 종료한다.

### 1-3. 실시간 시세 (REST → SSE 변환)
| 항목 | 내용 |
|------|------|
| **URL** | `GET /uapi/domestic-stock/v1/quotations/inquire-price` |
| **tr_id** | `FHKST01010100` |

> 서버에서 이 REST API를 주기적으로 호출하여 SseEmitter로 클라이언트에 push

**Query Parameters**

| 파라미터 | 설명 |
|----------|------|
| `FID_COND_MRKT_DIV_CODE` | 시장구분 (`"J"`: 주식) |
| `FID_INPUT_ISCD` | 종목코드 |

**Response**
```json
{
  "output": {
    "stck_prpr": "75500",      // 현재가 → currentPrice
    "prdy_vrss": "1500",       // 전일대비 → changePrice
    "prdy_ctrt": "2.03",       // 전일대비율 → changeRate
    "acml_vol": "12345678",    // 누적거래량 → volume
    "stck_hgpr": "76000",      // 고가 → high
    "stck_lwpr": "74000",      // 저가 → low
    "stck_oprc": "74500"       // 시가 → open
  }
}
```

---

## 2. 키움증권 (KIWOOM)

### Base URL
```
https://openapi.kiwoom.com
```

### 공통 요청 헤더
| 헤더 | 값 |
|------|-----|
| `Authorization` | `Bearer {access_token}` |
| `appkey` | 앱 키 |
| `appsecret` | 앱 시크릿 |
| `tr_id` | 각 API별 거래 ID |

### 2-1. 인증 (토큰 발급)
| 항목 | 내용 |
|------|------|
| **URL** | `POST /oauth2/token` |
| **Content-Type** | `application/x-www-form-urlencoded` |

**Request Parameters**

| 파라미터 | 설명 |
|----------|------|
| `grant_type` | `client_credentials` |
| `appkey` | 앱 키 |
| `appsecret` | 앱 시크릿 |

**Response**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9...",  // 접근토큰 → accessToken
  "token_type": "Bearer",                                       // 토큰타입 → tokenType
  "expires_in": 86400                                            // 만료시간(초) → expiresIn
}
```

### 2-2. 잔고 조회
| 항목 | 내용 |
|------|------|
| **URL** | `GET /api/dostk/acnt` (주식 계좌 잔고) |
| **tr_id** | `TTTC8434R` |

**Query Parameters**

| 파라미터 | 설명 |
|----------|------|
| `CANO` | 계좌번호 앞 8자리 |
| `ACNT_PRDT_CD` | 계좌상품코드 뒤 2자리 |
| `AFHR_FLPR_YN` | 시간외단일가여부 |
| `PRCS_DVSN` | 처리구분 (`"00"`: 전일매매포함) |

**Response**
```json
{
  "output1": [
    {
      "pdno": "005930",                // 종목코드 → stockCode
      "prdt_name": "삼성전자",          // 종목명 → stockName
      "hldg_qty": "100",              // 보유수량 → quantity
      "pchs_avg_pric": "72000.00",    // 매입평균가격 → avgPurchasePrice
      "evlu_amt": "7550000",          // 평가금액 → evaluation
      "evlu_pfls_amt": "350000",      // 평가손익금액 → profitLoss
      "evlu_pfls_rt": "4.86"          // 평가손익률 → profitRate
    }
  ],
  "output2": [
    {
      "tot_evlu_amt": "15230000",      // 총평가금액 → totalEvaluation
      "dnca_tot_amt": "5000000"        // 예수금총금액
    }
  ]
}
```

### 2-3. 실시간 시세 (REST → SSE 변환)
| 항목 | 내용 |
|------|------|
| **URL** | `GET /api/dostk/mrkt` (주식 현재가) |
| **tr_id** | `FHKST01010100` |

**Query Parameters**

| 파라미터 | 설명 |
|----------|------|
| `FID_COND_MRKT_DIV_CODE` | 시장구분 (`"J"`: 주식) |
| `FID_INPUT_ISCD` | 종목코드 |

**Response**
```json
{
  "output": {
    "stck_prpr": "75500",      // 현재가 → currentPrice
    "prdy_vrss": "1500",       // 전일대비 → changePrice
    "prdy_ctrt": "2.03",       // 전일대비율 → changeRate
    "acml_vol": "12345678",    // 누적거래량 → volume
    "stck_hgpr": "76000",      // 고가 → high
    "stck_lwpr": "74000",      // 저가 → low
    "stck_oprc": "74500"       // 시가 → open
  }
}
```

---

## 3. LS증권 (LS)

### Base URL
| 환경 | URL |
|------|-----|
| 실전투자 | `https://openapi.ls-sec.co.kr:8080` |

> 모의투자 계좌는 사용하지 않음. 실전투자 URL만 사용한다.

### 공통 요청 헤더
| 헤더 | 값 |
|------|-----|
| `Authorization` | `Bearer {access_token}` |
| `tr_cd` | TR 코드 |
| `tr_cont` | 연속조회 여부 (`"N"` / `"Y"`) |

### 3-1. 인증 (토큰 발급)
| 항목 | 내용 |
|------|------|
| **URL** | `POST /oauth2/token` |
| **Content-Type** | `application/x-www-form-urlencoded` |

**Request Parameters**

| 파라미터 | 설명 |
|----------|------|
| `grant_type` | `client_credentials` |
| `appkey` | 앱 키 |
| `appsecretkey` | 앱 시크릿 키 |
| `scope` | `oob` |

**Response**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9...",  // 접근토큰 → accessToken
  "token_type": "Bearer",                                       // 토큰타입 → tokenType
  "expires_in": 86400,                                           // 만료시간(초) → expiresIn
  "scope": "oob"                                                 // 권한범위 → scope
}
```

### 3-2. 잔고 조회
| 항목 | 내용 |
|------|------|
| **URL** | `POST /stock/accno` |
| **tr_cd** | `t0424` (주식 잔고 조회) |

**Request Body**
```json
{
  "t0424InBlock": {
    "accno": "계좌번호",
    "passwd": "비밀번호",
    "prcgb": "1",
    "chegb": "2",
    "dangb": "0",
    "charge": "1",
    "cts_expcode": ""
  }
}
```

**Response**
```json
{
  "t0424OutBlock": {
    "sunamt": "15230000",              // 추정순자산
    "dtsunik": "730000",               // 당일손익합계
    "mamt": "15230000"                 // 총평가금액
  },
  "t0424OutBlock1": [
    {
      "expcode": "005930",             // 종목코드 → stockCode
      "jangname": "삼성전자",           // 종목명 → stockName
      "janqty": "100",                // 잔고수량 → quantity
      "pamt": "72000",                // 매입가 → avgPurchasePrice
      "mamt": "75500",                // 현재가 → currentPrice
      "appamt": "7550000",            // 평가금액 → evaluation
      "dtsunik": "350000",            // 당일손익 → profitLoss
      "sunikrt": "4.86"               // 수익률 → profitRate
    }
  ]
}
```

### 3-3. 실시간 시세 (REST → SSE 변환)
| 항목 | 내용 |
|------|------|
| **URL** | `POST /stock/market-data` |
| **tr_cd** | `t1102` (주식 현재가) |

**Request Body**
```json
{
  "t1102InBlock": {
    "shcode": "005930"
  }
}
```

**Response**
```json
{
  "t1102OutBlock": {
    "hname": "삼성전자",               // 종목명
    "price": "75500",                 // 현재가 → currentPrice
    "sign": "2",                      // 전일대비부호 (2:상승)
    "change": "1500",                 // 전일대비 → changePrice
    "diff": "2.03",                   // 등락률 → changeRate
    "volume": "12345678",             // 거래량 → volume
    "high": "76000",                  // 고가 → high
    "low": "74000",                   // 저가 → low
    "open": "74500"                   // 시가 → open
  }
}
```

---

## 증권사별 API 비교 요약

### 인증 방식
| 증권사 | 토큰 발급 URL | 인증 방식 | 토큰 유효기간 |
|--------|-------------|-----------|-------------|
| KIS | `POST /oauth2/tokenP` | OAuth2 (appkey + appsecret) | 24시간 |
| 키움 | `POST /oauth2/token` | OAuth2 (appkey + appsecret) | 문서 확인 필요 |
| LS | `POST /oauth2/token` | OAuth2 (appkey + appsecretkey + scope) | 문서 확인 필요 |

### 잔고 조회
| 증권사 | URL | 메서드 | TR ID |
|--------|-----|--------|-------|
| KIS | `/uapi/domestic-stock/v1/trading/inquire-balance` | `GET` | `TTTC8434R` |
| 키움 | `/api/dostk/acnt` | `GET` | `TTTC8434R` |
| LS | `/stock/accno` | `POST` | `t0424` |

### 실시간 시세 (REST → SSE 변환용)
| 증권사 | URL | 메서드 | TR ID |
|--------|-----|--------|-------|
| KIS | `/uapi/domestic-stock/v1/quotations/inquire-price` | `GET` | `FHKST01010100` |
| 키움 | `/api/dostk/mrkt` | `GET` | `FHKST01010100` |
| LS | `/stock/market-data` | `POST` | `t1102` |

---

## 응답 필드 매핑 (공통 DTO ← 증권사별 응답)

### BalanceResponse (잔고)
| 공통 필드 | KIS | 키움 | LS |
|-----------|-----|------|-----|
| `stockCode` | `pdno` | `pdno` | `expcode` |
| `stockName` | `prdt_name` | `prdt_name` | `jangname` |
| `quantity` | `hldg_qty` | `hldg_qty` | `janqty` |
| `avgPurchasePrice` | `pchs_avg_pric` | `pchs_avg_pric` | `pamt` |
| `currentPrice` | `prpr` | — (별도 조회) | `mamt` |
| `evaluation` | `evlu_amt` | `evlu_amt` | `appamt` |
| `profitLoss` | `evlu_pfls_amt` | `evlu_pfls_amt` | `dtsunik` |
| `profitRate` | `evlu_pfls_rt` | `evlu_pfls_rt` | `sunikrt` |


### PriceResponse (시세)
| 공통 필드 | KIS | 키움 | LS |
|-----------|-----|------|-----|
| `currentPrice` | `stck_prpr` | `stck_prpr` | `price` |
| `changePrice` | `prdy_vrss` | `prdy_vrss` | `change` |
| `changeRate` | `prdy_ctrt` | `prdy_ctrt` | `diff` |
| `volume` | `acml_vol` | `acml_vol` | `volume` |
| `high` | `stck_hgpr` | `stck_hgpr` | `high` |
| `low` | `stck_lwpr` | `stck_lwpr` | `low` |
| `open` | `stck_oprc` | `stck_oprc` | `open` |
