# BROKER-API.md - 증권사별 API 호출 계획

> 아래 정보는 2025년 기준 공개 정보 기반이며, 실제 구현 전 공식 문서에서 최신 스펙을 반드시 재확인할 것.

---

## 1. 한국투자증권 (KIS)

### Base URL
| 환경 | URL |
|------|-----|
| 실전투자 | `https://openapi.koreainvestment.com:9443` |
| 모의투자 | `https://openapivts.koreainvestment.com:29443` |

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
| 필드 | 설명 |
|------|------|
| `access_token` | 접근 토큰 |
| `token_type` | `"Bearer"` |
| `expires_in` | 유효기간 (초, 86400 = 24시간) |

> 토큰 발급은 1분에 1회 제한. 24시간 유효.

### 1-2. 잔고 조회
| 항목 | 내용 |
|------|------|
| **URL** | `GET /uapi/domestic-stock/v1/trading/inquire-balance` |
| **tr_id** | `TTTC8434R` (실전) / `VTTC8434R` (모의) |

**Query Parameters**
| 파라미터 | 설명 |
|----------|------|
| `CANO` | 계좌번호 앞 8자리 |
| `ACNT_PRDT_CD` | 계좌상품코드 뒤 2자리 |
| `AFHR_FLPR_YN` | 시간외단일가여부 (`"N"`) |
| `INQR_DVSN` | 조회구분 (`"02"`: 종목별) |
| `UNPR_DVSN` | 단가구분 (`"01"`) |
| `FUND_STTL_ICLD_YN` | 펀드결제분포함여부 (`"N"`) |
| `FNCG_AMT_AUTO_RDPT_YN` | 융자금액자동상환여부 (`"N"`) |
| `PRCS_DVSN` | 처리구분 (`"00"`: 전일매매포함) |
| `CTX_AREA_FK100` | 연속조회검색조건 (최초 `""`) |
| `CTX_AREA_NK100` | 연속조회키 (최초 `""`) |

**Response — output1 (보유종목 리스트)**
| 필드 | 설명 | 매핑 |
|------|------|------|
| `pdno` | 종목코드 | → `stockCode` |
| `prdt_name` | 종목명 | → `stockName` |
| `hldg_qty` | 보유수량 | → `quantity` |
| `pchs_avg_pric` | 매입평균가격 | → `avgPurchasePrice` |
| `prpr` | 현재가 | → `currentPrice` |
| `evlu_amt` | 평가금액 | → `evaluation` |
| `evlu_pfls_amt` | 평가손익금액 | → `profitLoss` |
| `evlu_pfls_rt` | 평가손익율 | → `profitRate` |

**Response — output2 (계좌 요약)**
| 필드 | 설명 | 매핑 |
|------|------|------|
| `tot_evlu_amt` | 총평가금액 | → `totalEvaluation` |
| `pchs_amt_smtl_amt` | 매입금액합계 | → `totalPurchase` |
| `evlu_pfls_smtl_amt` | 평가손익합계 | → `totalProfitLoss` |

### 1-3. 거래내역 조회
| 항목 | 내용 |
|------|------|
| **URL** | `GET /uapi/domestic-stock/v1/trading/inquire-daily-ccld` |
| **tr_id** | `TTTC8001R` (실전) / `VTTC8001R` (모의) |

**Query Parameters**
| 파라미터 | 설명 |
|----------|------|
| `CANO` | 계좌번호 앞 8자리 |
| `ACNT_PRDT_CD` | 계좌상품코드 뒤 2자리 |
| `INQR_STRT_DT` | 조회시작일 (`YYYYMMDD`) |
| `INQR_END_DT` | 조회종료일 (`YYYYMMDD`) |
| `SLL_BUY_DVSN_CD` | 매매구분 (`"00"`: 전체, `"01"`: 매도, `"02"`: 매수) |
| `INQR_DVSN` | 조회구분 (`"00"`: 역순) |
| `PDNO` | 종목번호 (전체: `""`) |
| `CCLD_DVSN` | 체결구분 (`"01"`: 체결) |

**Response — output1 (체결 내역 리스트)**
| 필드 | 설명 | 매핑 |
|------|------|------|
| `ord_dt` | 주문일자 | → `transactionDate` |
| `ord_tmd` | 주문시각 | → `transactionTime` |
| `pdno` | 종목코드 | → `stockCode` |
| `prdt_name` | 종목명 | → `stockName` |
| `sll_buy_dvsn_cd` | 매매구분 (`01`:매도, `02`:매수) | → `tradeType` |
| `tot_ccld_qty` | 총체결수량 | → `quantity` |
| `avg_prvs` | 체결평균가 | → `price` |
| `tot_ccld_amt` | 총체결금액 | → `totalAmount` |

### 1-4. 실시간 시세 (REST → SSE 변환)
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
| 필드 | 설명 | 매핑 |
|------|------|------|
| `stck_prpr` | 현재가 | → `currentPrice` |
| `prdy_vrss` | 전일대비 | → `changePrice` |
| `prdy_ctrt` | 전일대비율 | → `changeRate` |
| `acml_vol` | 누적거래량 | → `volume` |
| `stck_hgpr` | 고가 | → `high` |
| `stck_lwpr` | 저가 | → `low` |
| `stck_oprc` | 시가 | → `open` |

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
| 필드 | 설명 |
|------|------|
| `access_token` | 접근 토큰 |
| `token_type` | `"Bearer"` |
| `expires_in` | 유효기간 (초) |

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
| 필드 | 설명 | 매핑 |
|------|------|------|
| `pdno` | 종목코드 | → `stockCode` |
| `prdt_name` | 종목명 | → `stockName` |
| `hldg_qty` | 보유수량 | → `quantity` |
| `pchs_avg_pric` | 매입평균가격 | → `avgPurchasePrice` |
| `evlu_amt` | 평가금액 | → `evaluation` |
| `evlu_pfls_amt` | 평가손익금액 | → `profitLoss` |
| `evlu_pfls_rt` | 평가손익률 | → `profitRate` |
| `tot_evlu_amt` | 총평가금액 | → `totalEvaluation` |
| `dnca_tot_amt` | 예수금총금액 | — |

### 2-3. 거래내역 조회
| 항목 | 내용 |
|------|------|
| **URL** | `GET /api/dostk/ord` (주문 체결 내역) |
| **tr_id** | `TTTC8001R` |

**Query Parameters**
| 파라미터 | 설명 |
|----------|------|
| `CANO` | 계좌번호 앞 8자리 |
| `ACNT_PRDT_CD` | 계좌상품코드 |
| `INQR_STRT_DT` | 조회시작일 (`YYYYMMDD`) |
| `INQR_END_DT` | 조회종료일 (`YYYYMMDD`) |
| `SLL_BUY_DVSN_CD` | 매매구분 (`"00"`: 전체, `"01"`: 매도, `"02"`: 매수) |
| `PDNO` | 종목코드 (전체: `""`) |

**Response**
| 필드 | 설명 | 매핑 |
|------|------|------|
| `ord_dt` | 주문일자 | → `transactionDate` |
| `ord_tmd` | 주문시각 | → `transactionTime` |
| `pdno` | 종목코드 | → `stockCode` |
| `prdt_name` | 종목명 | → `stockName` |
| `sll_buy_dvsn_cd` | 매매구분 | → `tradeType` |
| `ccld_qty` | 체결수량 | → `quantity` |
| `ccld_pric` | 체결가격 | → `price` |
| `tot_ccld_amt` | 총체결금액 | → `totalAmount` |

### 2-4. 실시간 시세 (REST → SSE 변환)
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
| 필드 | 설명 | 매핑 |
|------|------|------|
| `stck_prpr` | 현재가 | → `currentPrice` |
| `prdy_vrss` | 전일대비 | → `changePrice` |
| `prdy_ctrt` | 전일대비율 | → `changeRate` |
| `acml_vol` | 누적거래량 | → `volume` |
| `stck_hgpr` | 고가 | → `high` |
| `stck_lwpr` | 저가 | → `low` |
| `stck_oprc` | 시가 | → `open` |

---

## 3. LS증권 (LS)

### Base URL
| 환경 | URL |
|------|-----|
| 실전투자 | `https://openapi.ls-sec.co.kr:8080` |
| 모의투자 | `https://openapi.ls-sec.co.kr:29443` |

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
| 필드 | 설명 |
|------|------|
| `access_token` | 접근 토큰 |
| `token_type` | `"Bearer"` |
| `expires_in` | 유효기간 (초) |

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

**Response — t0424OutBlock1 (보유종목 리스트)**
| 필드 | 설명 | 매핑 |
|------|------|------|
| `expcode` | 종목코드 | → `stockCode` |
| `jangname` | 종목명 | → `stockName` |
| `janqty` | 잔고수량 | → `quantity` |
| `pamt` | 매입가 | → `avgPurchasePrice` |
| `mamt` | 현재가 | → `currentPrice` |
| `appamt` | 평가금액 | → `evaluation` |
| `dtsunik` | 당일손익 | → `profitLoss` |
| `sunikrt` | 수익률 | → `profitRate` |

### 3-3. 거래내역 조회
| 항목 | 내용 |
|------|------|
| **URL** | `POST /stock/accno` |
| **tr_cd** | `CSPAQ13700` 또는 `t0150` |

**Request Body (CSPAQ13700)**
```json
{
  "CSPAQ13700InBlock1": {
    "AcntNo": "계좌번호",
    "Pwd": "비밀번호",
    "QrySrtDt": "20260301",
    "QryEndDt": "20260314",
    "BnsTpCode": "0",
    "SrtNo": ""
  }
}
```

**Response — CSPAQ13700OutBlock3 (체결 내역 리스트)**
| 필드 | 설명 | 매핑 |
|------|------|------|
| `OrdDt` | 주문일자 | → `transactionDate` |
| `CcldTm` | 체결시각 | → `transactionTime` |
| `IsuNo` | 종목코드 | → `stockCode` |
| `IsuNm` | 종목명 | → `stockName` |
| `BnsTpNm` | 매매구분명 | → `tradeType` |
| `CcldQty` | 체결수량 | → `quantity` |
| `CcldPrc` | 체결단가 | → `price` |
| `CcldAmt` | 체결금액 | → `totalAmount` |

### 3-4. 실시간 시세 (REST → SSE 변환)
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

**Response — t1102OutBlock**
| 필드 | 설명 | 매핑 |
|------|------|------|
| `price` | 현재가 | → `currentPrice` |
| `change` | 전일대비 | → `changePrice` |
| `diff` | 등락률 | → `changeRate` |
| `volume` | 거래량 | → `volume` |
| `high` | 고가 | → `high` |
| `low` | 저가 | → `low` |
| `open` | 시가 | → `open` |

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

### 거래내역 조회
| 증권사 | URL | 메서드 | TR ID |
|--------|-----|--------|-------|
| KIS | `/uapi/domestic-stock/v1/trading/inquire-daily-ccld` | `GET` | `TTTC8001R` |
| 키움 | `/api/dostk/ord` | `GET` | `TTTC8001R` |
| LS | `/stock/accno` | `POST` | `CSPAQ13700` |

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

### TransactionResponse (거래내역)
| 공통 필드 | KIS | 키움 | LS |
|-----------|-----|------|-----|
| `transactionDate` | `ord_dt` | `ord_dt` | `OrdDt` |
| `transactionTime` | `ord_tmd` | `ord_tmd` | `CcldTm` |
| `stockCode` | `pdno` | `pdno` | `IsuNo` |
| `stockName` | `prdt_name` | `prdt_name` | `IsuNm` |
| `tradeType` | `sll_buy_dvsn_cd` | `sll_buy_dvsn_cd` | `BnsTpNm` |
| `quantity` | `tot_ccld_qty` | `ccld_qty` | `CcldQty` |
| `price` | `avg_prvs` | `ccld_pric` | `CcldPrc` |
| `totalAmount` | `tot_ccld_amt` | `tot_ccld_amt` | `CcldAmt` |

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
