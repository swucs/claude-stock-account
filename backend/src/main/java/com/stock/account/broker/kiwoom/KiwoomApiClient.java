package com.stock.account.broker.kiwoom;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.dto.BalanceResponse;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockHolding;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.broker.kis.KisAccountUtil;
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
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private static final String TR_ID_BALANCE = "TTTC8434R";
    private static final String TR_ID_PRICE = "FHKST01010100";

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
            // 키움은 application/x-www-form-urlencoded 방식으로 토큰 발급
            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formParams.add("grant_type", "client_credentials");
            formParams.add("appkey", appKey);
            formParams.add("appsecret", secretKey);

            KiwoomTokenApiResponse response = brokerRestClient.post()
                    .uri(baseUrl + tokenPath)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formParams)
                    .retrieve()
                    .body(KiwoomTokenApiResponse.class);

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
        String appKey = aesEncryptionUtil.decrypt(account.getAppKey());
        String secretKey = aesEncryptionUtil.decrypt(account.getSecretKey());
        String baseUrl = brokerProperties.getKiwoom().getBaseUrl();
        String balancePath = brokerProperties.getKiwoom().getBalancePath();

        String cano = KisAccountUtil.parseCano(account.getAccountNumber());
        String acntPrdtCd = KisAccountUtil.parseAcntPrdtCd(account.getAccountNumber());

        try {
            KiwoomBalanceApiResponse response = brokerRestClient.get()
                    .uri(baseUrl + balancePath, uriBuilder -> uriBuilder
                            .queryParam("CANO", cano)
                            .queryParam("ACNT_PRDT_CD", acntPrdtCd)
                            .queryParam("AFHR_FLPR_YN", "N")
                            .queryParam("PRCS_DVSN", "00")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                    .header("appkey", appKey)
                    .header("appsecret", secretKey)
                    .header("tr_id", TR_ID_BALANCE)
                    .retrieve()
                    .body(KiwoomBalanceApiResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.BROKER_API_ERROR, "키움 잔고 API 응답이 비어있습니다.");
            }

            List<StockHolding> holdings = new ArrayList<>();
            if (response.output1() != null) {
                for (KiwoomBalanceItem item : response.output1()) {
                    if (item.hldgQty() != null && Long.parseLong(item.hldgQty()) > 0) {
                        holdings.add(mapToStockHolding(item));
                    }
                }
            }

            BigDecimal totalEval = BigDecimal.ZERO;
            if (response.output2() != null && !response.output2().isEmpty()) {
                totalEval = parseBigDecimal(response.output2().getFirst().totEvluAmt());
            }

            return new BalanceResponse(
                    account.getId(),
                    account.getAccountNumber(),
                    account.getBrokerType().getDisplayName(),
                    totalEval,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    holdings
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
        String appKey = aesEncryptionUtil.decrypt(account.getAppKey());
        String secretKey = aesEncryptionUtil.decrypt(account.getSecretKey());
        String baseUrl = brokerProperties.getKiwoom().getBaseUrl();
        String pricePath = brokerProperties.getKiwoom().getPricePath();

        try {
            KiwoomPriceApiResponse response = brokerRestClient.get()
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
                    .body(KiwoomPriceApiResponse.class);

            if (response == null || response.output() == null) {
                throw new BusinessException(ErrorCode.BROKER_API_ERROR, "키움 시세 API 응답이 비어있습니다.");
            }

            return mapToPriceResponse(stockCode, response.output());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("키움 시세 조회 실패 - stockCode: {}", stockCode, e);
            throw new BusinessException(ErrorCode.BROKER_API_ERROR, e.getMessage());
        }
    }

    private StockHolding mapToStockHolding(KiwoomBalanceItem item) {
        // 키움 잔고 응답에는 현재가(prpr)가 없으므로 0으로 설정
        return new StockHolding(
                item.pdno(),
                item.prdtName(),
                Long.parseLong(item.hldgQty()),
                parseBigDecimal(item.pchsAvgPric()),
                BigDecimal.ZERO,
                parseBigDecimal(item.evluAmt()),
                parseBigDecimal(item.evluPflsAmt()),
                parseBigDecimal(item.evluPflsRt())
        );
    }

    private StockPriceResponse mapToPriceResponse(String stockCode, KiwoomPriceOutput output) {
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
