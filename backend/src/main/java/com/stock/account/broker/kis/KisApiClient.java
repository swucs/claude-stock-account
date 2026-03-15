package com.stock.account.broker.kis;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.dto.*;
import com.stock.account.broker.kis.dto.*;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisApiClient implements BrokerApiClient {

    private final RestClient brokerRestClient;
    private final BrokerProperties brokerProperties;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final KisTokenCache tokenCache;

    // 실전투자용 tr_id
    private static final String TR_ID_BALANCE = "TTTC8434R";
    private static final String TR_ID_PRICE = "FHKST01010100";

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.KIS;
    }

    @Override
    public BrokerTokenResponse authenticate(Account account) {
        // 캐시에서 유효한 토큰 확인
        BrokerTokenResponse cached = tokenCache.getToken(account.getId());
        if (cached != null) {
            return cached;
        }

        String appKey = aesEncryptionUtil.decrypt(account.getAppKey());
        String secretKey = aesEncryptionUtil.decrypt(account.getSecretKey());
        String baseUrl = brokerProperties.getKis().getBaseUrl();
        String tokenPath = brokerProperties.getKis().getTokenPath();

        try {
            KisTokenApiResponse response = brokerRestClient.post()
                    .uri(baseUrl + tokenPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(KisTokenRequest.of(appKey, secretKey))
                    .retrieve()
                    .body(KisTokenApiResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new BusinessException(ErrorCode.BROKER_AUTH_FAILED);
            }

            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(response.expiresIn());
            BrokerTokenResponse tokenResponse = new BrokerTokenResponse(
                    response.accessToken(),
                    response.tokenType(),
                    expiresAt
            );

            tokenCache.putToken(account.getId(), tokenResponse);
            log.info("KIS 토큰 발급 성공 - accountId: {}", account.getId());

            return tokenResponse;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("KIS 토큰 발급 실패 - accountId: {}", account.getId(), e);
            throw new BusinessException(ErrorCode.BROKER_AUTH_FAILED, e.getMessage());
        }
    }

    @Override
    public BalanceResponse getBalance(Account account, String accessToken) {
        String appKey = aesEncryptionUtil.decrypt(account.getAppKey());
        String secretKey = aesEncryptionUtil.decrypt(account.getSecretKey());
        String baseUrl = brokerProperties.getKis().getBaseUrl();
        String balancePath = brokerProperties.getKis().getBalancePath();

        String cano = KisAccountUtil.parseCano(account.getAccountNumber());
        String acntPrdtCd = KisAccountUtil.parseAcntPrdtCd(account.getAccountNumber());

        List<StockHolding> allHoldings = new ArrayList<>();
        String ctxAreaFk100 = "";
        String ctxAreaNk100 = "";
        KisBalanceSummary summary = null;

        try {
            // 연속조회 루프
            boolean hasMore = true;
            while (hasMore) {
                String fk100 = ctxAreaFk100;
                String nk100 = ctxAreaNk100;
                String trCont = fk100.isEmpty() ? "" : "N";

                ResponseEntity<KisBalanceApiResponse> responseEntity = brokerRestClient.get()
                        .uri(baseUrl + balancePath, uriBuilder -> uriBuilder
                                .queryParam("CANO", cano)
                                .queryParam("ACNT_PRDT_CD", acntPrdtCd)
                                .queryParam("AFHR_FLPR_YN", "N")
                                .queryParam("OFL_YN", "")
                                .queryParam("INQR_DVSN", "02")
                                .queryParam("UNPR_DVSN", "01")
                                .queryParam("FUND_STTL_ICLD_YN", "N")
                                .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                                .queryParam("PRCS_DVSN", "00")
                                .queryParam("CTX_AREA_FK100", fk100)
                                .queryParam("CTX_AREA_NK100", nk100)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                        .header("appkey", appKey)
                        .header("appsecret", secretKey)
                        .header("tr_id", TR_ID_BALANCE)
                        .header("tr_cont", trCont)
                        .header("custtype", "P")
                        .retrieve()
                        .toEntity(KisBalanceApiResponse.class);

                KisBalanceApiResponse body = responseEntity.getBody();
                if (body == null) {
                    throw new BusinessException(ErrorCode.BROKER_API_ERROR, "KIS 잔고 API 응답이 비어있습니다.");
                }

                // 종목 매핑
                if (body.output1() != null) {
                    for (KisBalanceItem item : body.output1()) {
                        if (item.hldgQty() != null && Long.parseLong(item.hldgQty()) > 0) {
                            allHoldings.add(mapToStockHolding(item));
                        }
                    }
                }

                // 요약 (첫 번째 응답에서만 사용)
                if (summary == null && body.output2() != null && !body.output2().isEmpty()) {
                    summary = body.output2().getFirst();
                }

                // 연속조회 확인
                String trContResponse = responseEntity.getHeaders().getFirst("tr_cont");
                if ("F".equals(trContResponse) || "M".equals(trContResponse)) {
                    ctxAreaFk100 = body.ctxAreaFk100() != null ? body.ctxAreaFk100() : "";
                    ctxAreaNk100 = body.ctxAreaNk100() != null ? body.ctxAreaNk100() : "";
                } else {
                    hasMore = false;
                }
            }

            BigDecimal totalEval = BigDecimal.ZERO;
            BigDecimal totalPurchase = BigDecimal.ZERO;
            BigDecimal totalProfitLoss = BigDecimal.ZERO;

            if (summary != null) {
                totalEval = parseBigDecimal(summary.totEvluAmt());
                totalPurchase = parseBigDecimal(summary.pchsAmtSmtlAmt());
                totalProfitLoss = parseBigDecimal(summary.evluPflsSmtlAmt());
            }

            return new BalanceResponse(
                    account.getId(),
                    account.getAccountNumber(),
                    account.getBrokerType().getDisplayName(),
                    totalEval,
                    totalPurchase,
                    totalProfitLoss,
                    allHoldings
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("KIS 잔고 조회 실패 - accountId: {}", account.getId(), e);
            throw new BusinessException(ErrorCode.BROKER_API_ERROR, e.getMessage());
        }
    }

    @Override
    public StockPriceResponse getCurrentPrice(Account account, String accessToken, String stockCode) {
        String appKey = aesEncryptionUtil.decrypt(account.getAppKey());
        String secretKey = aesEncryptionUtil.decrypt(account.getSecretKey());
        String baseUrl = brokerProperties.getKis().getBaseUrl();
        String pricePath = brokerProperties.getKis().getPricePath();

        try {
            KisPriceApiResponse response = brokerRestClient.get()
                    .uri(baseUrl + pricePath, uriBuilder -> uriBuilder
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                    .header("appkey", appKey)
                    .header("appsecret", secretKey)
                    .header("tr_id", TR_ID_PRICE)
                    .retrieve()
                    .body(KisPriceApiResponse.class);

            if (response == null || response.output() == null) {
                throw new BusinessException(ErrorCode.BROKER_API_ERROR, "KIS 시세 API 응답이 비어있습니다.");
            }

            return mapToPriceResponse(stockCode, response.output());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("KIS 시세 조회 실패 - stockCode: {}", stockCode, e);
            throw new BusinessException(ErrorCode.BROKER_API_ERROR, e.getMessage());
        }
    }

    private StockHolding mapToStockHolding(KisBalanceItem item) {
        return new StockHolding(
                item.pdno(),
                item.prdtName(),
                Long.parseLong(item.hldgQty()),
                parseBigDecimal(item.pchsAvgPric()),
                parseBigDecimal(item.prpr()),
                parseBigDecimal(item.evluAmt()),
                parseBigDecimal(item.evluPflsAmt()),
                parseBigDecimal(item.evluPflsRt())
        );
    }

    private StockPriceResponse mapToPriceResponse(String stockCode, KisPriceOutput output) {
        return new StockPriceResponse(
                stockCode,
                output.htsKorIsnm() != null ? output.htsKorIsnm() : "",
                parseBigDecimal(output.stckPrpr()),
                parseBigDecimal(output.prdyVrss()),
                parseBigDecimal(output.prdyCtrt()),
                parseLong(output.acmlVol()),
                parseBigDecimal(output.stckHgpr()),
                parseBigDecimal(output.stckLwpr()),
                parseBigDecimal(output.stckOprc())
        );
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
