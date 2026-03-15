package com.stock.account.broker.kiwoom;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.dto.BalanceResponse;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockHolding;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.broker.kiwoom.dto.*;
import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.common.util.AesEncryptionUtil;
import com.stock.account.config.BrokerProperties;
import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomApiClient implements BrokerApiClient {

    private final RestClient brokerRestClient;
    private final BrokerProperties brokerProperties;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final KiwoomTokenCache tokenCache;

    // 키움 API ID (KIS의 tr_id에 해당)
    private static final String API_ID_BALANCE = "kt00018";  // 계좌평가잔고내역요청
    private static final String API_ID_PRICE = "ka10095";    // 관심종목정보요청

    // 키움 expires_dt 응답 형식: "yyyyMMddHHmmss"
    private static final DateTimeFormatter EXPIRES_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 종목코드 A-prefix (KRX 국내주식)
    private static final String KRX_PREFIX = "A";

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.KIWOOM;
    }

    @Override
    public BrokerTokenResponse authenticate(Account account) {
        BrokerTokenResponse cached = tokenCache.getToken(account.getId());
        if (cached != null) {
            return cached;
        }

        String appKey = aesEncryptionUtil.decrypt(account.getAppKey());
        String secretKey = aesEncryptionUtil.decrypt(account.getSecretKey());
        String baseUrl = brokerProperties.getKiwoom().getBaseUrl();
        String tokenPath = brokerProperties.getKiwoom().getTokenPath();

        try {
            // 키움은 JSON body로 토큰 발급 (appkey, secretkey 파라미터명)
            KiwoomTokenApiResponse response = brokerRestClient.post()
                    .uri(baseUrl + tokenPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(KiwoomTokenRequest.of(appKey, secretKey))
                    .retrieve()
                    .body(KiwoomTokenApiResponse.class);

            if (response == null || response.token() == null) {
                throw new BusinessException(ErrorCode.BROKER_AUTH_FAILED);
            }

            if (response.returnCode() != null && response.returnCode() != 0) {
                throw new BusinessException(ErrorCode.BROKER_AUTH_FAILED,
                        "키움 토큰 발급 실패: " + response.returnMsg());
            }

            // expires_dt: "yyyyMMddHHmmss" 형식
            LocalDateTime expiresAt = LocalDateTime.parse(response.expiresDt(), EXPIRES_FORMATTER);
            BrokerTokenResponse tokenResponse = new BrokerTokenResponse(
                    response.token(),
                    response.tokenType() != null ? response.tokenType() : "bearer",
                    expiresAt
            );

            tokenCache.putToken(account.getId(), tokenResponse);
            log.info("키움 토큰 발급 성공 - accountId: {}", account.getId());

            return tokenResponse;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("키움 토큰 발급 실패 - accountId: {}", account.getId(), e);
            throw new BusinessException(ErrorCode.BROKER_AUTH_FAILED, e.getMessage());
        }
    }

    @Override
    public BalanceResponse getBalance(Account account, String accessToken) {
        String baseUrl = brokerProperties.getKiwoom().getBaseUrl();
        String balancePath = brokerProperties.getKiwoom().getBalancePath();

        List<StockHolding> allHoldings = new ArrayList<>();
        String totEvltAmt = "0";
        String totPurAmt = "0";
        String totEvltPl = "0";
        String contYn = "";
        String nextKey = "";

        try {
            // cont-yn/next-key 헤더 기반 연속조회 루프
            boolean hasMore = true;
            while (hasMore) {
                String finalContYn = contYn;
                String finalNextKey = nextKey;

                ResponseEntity<KiwoomBalanceApiResponse> responseEntity = brokerRestClient.post()
                        .uri(baseUrl + balancePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("api-id", API_ID_BALANCE)
                        .header("cont-yn", finalContYn)
                        .header("next-key", finalNextKey)
                        .body(KiwoomBalanceRequest.defaultRequest())
                        .retrieve()
                        .toEntity(KiwoomBalanceApiResponse.class);

                KiwoomBalanceApiResponse body = responseEntity.getBody();
                if (body == null) {
                    throw new BusinessException(ErrorCode.BROKER_API_ERROR, "키움 잔고 API 응답이 비어있습니다.");
                }

                // 종목 매핑
                if (body.holdings() != null) {
                    for (KiwoomBalanceItem item : body.holdings()) {
                        if (item.rmndQty() != null && Long.parseLong(item.rmndQty()) > 0) {
                            allHoldings.add(mapToStockHolding(item));
                        }
                    }
                }

                // 요약은 첫 번째 응답에서만 사용
                if (contYn.isEmpty()) {
                    totEvltAmt = body.totEvltAmt() != null ? body.totEvltAmt() : "0";
                    totPurAmt = body.totPurAmt() != null ? body.totPurAmt() : "0";
                    totEvltPl = body.totEvltPl() != null ? body.totEvltPl() : "0";
                }

                // 연속조회 확인 (응답 헤더)
                String respContYn = responseEntity.getHeaders().getFirst("cont-yn");
                if ("Y".equals(respContYn)) {
                    contYn = "Y";
                    nextKey = responseEntity.getHeaders().getFirst("next-key");
                    if (nextKey == null) nextKey = "";
                } else {
                    hasMore = false;
                }
            }

            return new BalanceResponse(
                    account.getId(),
                    account.getAccountNumber(),
                    account.getBrokerType().getDisplayName(),
                    parseBigDecimal(totEvltAmt),
                    parseBigDecimal(totPurAmt),
                    parseBigDecimal(totEvltPl),
                    allHoldings
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("키움 잔고 조회 실패 - accountId: {}", account.getId(), e);
            throw new BusinessException(ErrorCode.BROKER_API_ERROR, e.getMessage());
        }
    }

    @Override
    public StockPriceResponse getCurrentPrice(Account account, String accessToken, String stockCode) {
        String baseUrl = brokerProperties.getKiwoom().getBaseUrl();
        String pricePath = brokerProperties.getKiwoom().getPricePath();

        // 키움 종목코드는 A-prefix 필요 (KRX 국내주식)
        String kiwoomStockCode = stockCode.startsWith(KRX_PREFIX)
                ? stockCode : KRX_PREFIX + stockCode;

        try {
            KiwoomPriceApiResponse response = brokerRestClient.post()
                    .uri(baseUrl + pricePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("api-id", API_ID_PRICE)
                    .body(new KiwoomPriceRequest(kiwoomStockCode))
                    .retrieve()
                    .body(KiwoomPriceApiResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                throw new BusinessException(ErrorCode.BROKER_API_ERROR, "키움 시세 API 응답이 비어있습니다.");
            }

            KiwoomPriceOutput output = response.items().getFirst();
            return mapToPriceResponse(stockCode, output);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("키움 시세 조회 실패 - stockCode: {}", stockCode, e);
            throw new BusinessException(ErrorCode.BROKER_API_ERROR, e.getMessage());
        }
    }

    private StockHolding mapToStockHolding(KiwoomBalanceItem item) {
        // stk_cd에서 A-prefix 제거 (예: "A005930" → "005930")
        String stockCode = item.stkCd() != null && item.stkCd().startsWith(KRX_PREFIX)
                ? item.stkCd().substring(1) : item.stkCd();

        return new StockHolding(
                stockCode,
                item.stkNm(),
                parseLong(item.rmndQty()),
                parseAbsoluteBigDecimal(item.purPric()),
                parseAbsoluteBigDecimal(item.curPrc()),   // 키움은 잔고에 현재가 포함
                parseAbsoluteBigDecimal(item.evltAmt()),
                parseBigDecimal(item.elvtvPrft()),
                parseBigDecimal(item.prftRt())
        );
    }

    private StockPriceResponse mapToPriceResponse(String stockCode, KiwoomPriceOutput output) {
        return new StockPriceResponse(
                stockCode,
                output.stkNm() != null ? output.stkNm() : "",
                parseAbsoluteBigDecimal(output.curPrc()),
                parseBigDecimal(output.predPre()),
                parseBigDecimal(output.fluRt()),
                parseLong(output.trdeQty()),
                BigDecimal.ZERO,   // ka10095 응답에 고가 없음
                BigDecimal.ZERO,   // ka10095 응답에 저가 없음
                BigDecimal.ZERO    // ka10095 응답에 시가 없음
        );
    }

    /**
     * 키움 API는 가격 필드에 부호 접두사를 붙이는 경우가 있음 (예: "-20800").
     * 현재가, 매입가, 평가금액 등 절댓값이 필요한 필드에 사용.
     */
    private BigDecimal parseAbsoluteBigDecimal(String value) {
        return parseBigDecimal(value).abs();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
