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
https://api.kiwoom.com
```

### 공통 요청 헤더
| 헤더 | 값 |
|------|-----|
| `Authorization` | `Bearer {access_token}` |
| `api-id` | 각 API별 고유 ID (KIS의 tr_id에 해당) |
| `cont-yn` | 연속조회여부 (`""` 최초 / `"Y"` 연속) |
| `next-key` | 연속조회키 (응답에서 수신한 값) |

### 2-1. 인증 (토큰 발급) — au10001
| 항목 | 내용 |
|------|------|
| **URL** | `POST /oauth2/token` |
| **Content-Type** | `application/json;charset=UTF-8` |

**Request Body**
```json
{
  "grant_type": "client_credentials",
  "appkey": "{앱키}",
  "secretkey": "{시크릿키}"
}
```

**Response**
```json
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9...",  // 접근토큰 → token
  "token_type": "bearer",                               // 토큰타입
  "expires_dt": "20261231235959",                       // 만료일시 (yyyyMMddHHmmss) → expiresAt
  "return_code": 0,                                     // 0: 정상
  "return_msg": "정상"
}
```

### 2-2. 잔고 조회 — kt00018 (계좌평가잔고내역요청)
| 항목 | 내용 |
|------|------|
| **URL** | `POST /api/dostk/acnt` |
| **api-id** | `kt00018` |

> 계좌번호를 별도로 전달하지 않음 — 토큰 자체에 계좌 컨텍스트 내포

**Request Body**
```json
{
  "qry_tp": "2",        // "1":합산, "2":개별
  "dmst_stex_tp": "KRX" // "KRX":한국거래소, "NXT":넥스트트레이드
}
```

**Response**
```json
{
  "tot_pur_amt": "14500000",           // 총매입금액 → totalPurchase
  "tot_evlt_amt": "15230000",          // 총평가금액 → totalEvaluation
  "tot_evlt_pl": "730000",             // 총평가손익 → totalProfitLoss
  "tot_prft_rt": "5.03",               // 총수익률(%)
  "acnt_evlt_remn_indv_tot": [         // 계좌평가잔고 개별 목록
    {
      "stk_cd": "A005930",             // 종목번호 (A-prefix 포함) → stockCode (A 제거)
      "stk_nm": "삼성전자",             // 종목명 → stockName
      "cur_prc": "75500",              // 현재가 → currentPrice (부호 접두사 가능)
      "rmnd_qty": "100",               // 잔고수량 → quantity
      "trde_able_qty": "100",          // 매매가능수량
      "pur_pric": "72000",             // 매입가 → avgPurchasePrice
      "pur_amt": "7200000",            // 매입금액
      "evlt_amt": "7550000",           // 평가금액 → evaluation
      "evltv_prft": "350000",          // 평가손익 → profitLoss
      "prft_rt": "4.86"               // 수익률(%) → profitRate
    }
  ]
}
```

**연속조회 흐름**
> 응답 헤더 `cont-yn: Y`이면 다음 요청 시 요청 헤더 `cont-yn: Y`, `next-key: {응답 헤더값}`을 설정하여 반복 호출.

### 2-3. 현재가 조회 (REST → SSE 변환) — ka10095 (관심종목정보요청)
| 항목 | 내용 |
|------|------|
| **URL** | `POST /api/dostk/stkinfo` |
| **api-id** | `ka10095` |

**Request Body**
```json
{
  "stk_cd": "A005930"   // 종목코드 (A-prefix 포함, 여러 개는 | 구분)
}
```

**Response**
```json
{
  "atn_stk_infr": [
    {
      "stk_cd": "A005930",   // 종목코드 → stockCode
      "stk_nm": "삼성전자",   // 종목명 → stockName
      "cur_prc": "75500",    // 현재가 → currentPrice
      "pred_pre": "1500",    // 전일대비 → changePrice
      "pred_pre_sig": "2",   // 전일대비기호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
      "flu_rt": "2.03",      // 등락율(%) → changeRate
      "trde_qty": "12345678",// 거래량 → volume
      "sel_bid": "75600",    // 매도호가
      "buy_bid": "75500"     // 매수호가
    }
  ]
}
```

> 고가/저가/시가는 ka10095에 없음 → `high`, `low`, `open` = 0으로 설정

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
| 증권사 | 토큰 발급 URL | Content-Type | 인증 방식 | 만료 방식 |
|--------|-------------|-------------|-----------|----------|
| KIS | `POST /oauth2/tokenP` | JSON | appkey + appsecret | `expires_in`(초) |
| 키움 | `POST /oauth2/token` | JSON | appkey + **secretkey** | `expires_dt`(yyyyMMddHHmmss) |
| LS | `POST /oauth2/token` | form-urlencoded | appkey + appsecretkey + scope | `expires_in`(초) |

### 잔고 조회
| 증권사 | URL | 메서드 | API ID | 계좌번호 전달 |
|--------|-----|--------|--------|------------|
| KIS | `/uapi/domestic-stock/v1/trading/inquire-balance` | `GET` | `TTTC8434R` (tr_id 헤더) | 쿼리 파라미터 CANO/ACNT_PRDT_CD |
| 키움 | `/api/dostk/acnt` | `POST` | `kt00018` (api-id 헤더) | 불필요 (토큰에 내포) |
| LS | `/stock/accno` | `POST` | `t0424` (tr_cd 헤더) | Request Body |

### 현재가 조회 (REST polling → SSE)
| 증권사 | URL | 메서드 | API ID |
|--------|-----|--------|--------|
| KIS | `/uapi/domestic-stock/v1/quotations/inquire-price` | `GET` | `FHKST01010100` (tr_id) |
| 키움 | `/api/dostk/stkinfo` | `POST` | `ka10095` (api-id) |
| LS | `/stock/market-data` | `POST` | `t1102` (tr_cd) |

---

## 응답 필드 매핑 (공통 DTO ← 증권사별 응답)

### BalanceResponse (잔고)
| 공통 필드 | KIS | 키움 | LS |
|-----------|-----|------|-----|
| `stockCode` | `output1[].pdno` | `acnt_evlt_remn_indv_tot[].stk_cd` (A prefix 제거) | `t0424OutBlock1[].expcode` |
| `stockName` | `output1[].prdt_name` | `stk_nm` | `jangname` |
| `quantity` | `output1[].hldg_qty` | `rmnd_qty` | `janqty` |
| `avgPurchasePrice` | `output1[].pchs_avg_pric` | `pur_pric` | `pamt` |
| `currentPrice` | `output1[].prpr` | `cur_prc` (잔고에 포함) | `mamt` |
| `evaluation` | `output1[].evlu_amt` | `evlt_amt` | `appamt` |
| `profitLoss` | `output1[].evlu_pfls_amt` | `evltv_prft` | `dtsunik` |
| `profitRate` | `output1[].evlu_pfls_rt` | `prft_rt` | `sunikrt` |
| `totalEvaluation` | `output2[].tot_evlu_amt` | `tot_evlt_amt` | `t0424OutBlock.mamt` |
| `totalPurchase` | `output2[].pchs_amt_smtl_amt` | `tot_pur_amt` | — |
| `totalProfitLoss` | `output2[].evlu_pfls_smtl_amt` | `tot_evlt_pl` | `t0424OutBlock.dtsunik` |


### PriceResponse (시세)
| 공통 필드 | KIS | 키움 | LS |
|-----------|-----|------|-----|
| `currentPrice` | `output.stck_prpr` | `atn_stk_infr[].cur_prc` | `t1102OutBlock.price` |
| `changePrice` | `output.prdy_vrss` | `pred_pre` | `change` |
| `changeRate` | `output.prdy_ctrt` | `flu_rt` | `diff` |
| `volume` | `output.acml_vol` | `trde_qty` | `volume` |
| `high` | `output.stck_hgpr` | — (0) | `high` |
| `low` | `output.stck_lwpr` | — (0) | `low` |
| `open` | `output.stck_oprc` | — (0) | `open` |
