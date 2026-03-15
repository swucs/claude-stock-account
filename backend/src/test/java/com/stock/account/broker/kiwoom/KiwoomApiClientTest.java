package com.stock.account.broker.kiwoom;

import com.stock.account.broker.dto.BalanceResponse;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.broker.kiwoom.dto.*;
import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.common.util.AesEncryptionUtil;
import com.stock.account.config.BrokerProperties;
import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KiwoomApiClientTest {

    @Mock
    private RestClient brokerRestClient;

    @Mock
    private BrokerProperties brokerProperties;

    @Mock
    private AesEncryptionUtil aesEncryptionUtil;

    @Mock
    private KiwoomTokenCache tokenCache;

    @Mock
    private BrokerProperties.BrokerEndpoint kiwoomProperties;

    private KiwoomApiClient kiwoomApiClient;

    @BeforeEach
    void setUp() {
        kiwoomApiClient = new KiwoomApiClient(brokerRestClient, brokerProperties, aesEncryptionUtil, tokenCache);
    }

    @Test
    @DisplayName("getBrokerType() - KIWOOM 반환")
    void getBrokerType() {
        assertThat(kiwoomApiClient.getBrokerType()).isEqualTo(BrokerType.KIWOOM);
    }

    @Nested
    @DisplayName("authenticate() 테스트")
    class AuthenticateTests {

        @Test
        @DisplayName("캐시에 유효한 토큰이 있으면 API 호출 없이 반환")
        void authenticateWithCachedToken() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);

            BrokerTokenResponse cachedToken = new BrokerTokenResponse(
                    "cached-token", "Bearer", LocalDateTime.now().plusHours(12));
            when(tokenCache.getToken(1L)).thenReturn(cachedToken);

            BrokerTokenResponse result = kiwoomApiClient.authenticate(account);

            assertThat(result.accessToken()).isEqualTo("cached-token");
            verify(brokerRestClient, never()).post();
        }

        @Test
        @DisplayName("캐시 미스 시 키움 API 호출하여 토큰 발급 (form-urlencoded)")
        void authenticateWithApiCall() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(tokenCache.getToken(1L)).thenReturn(null);
            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getTokenPath()).thenReturn("/oauth2/token");

            KiwoomTokenApiResponse apiResponse = new KiwoomTokenApiResponse(
                    "new-access-token", "Bearer", 86400);

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.body(any(LinkedMultiValueMap.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomTokenApiResponse.class)).thenReturn(apiResponse);

            BrokerTokenResponse result = kiwoomApiClient.authenticate(account);

            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.tokenType()).isEqualTo("Bearer");
            verify(tokenCache).putToken(eq(1L), any(BrokerTokenResponse.class));
        }

        @Test
        @DisplayName("API 응답이 null이면 BROKER_AUTH_FAILED 예외")
        void authenticateNullResponse() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(tokenCache.getToken(1L)).thenReturn(null);
            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getTokenPath()).thenReturn("/oauth2/token");

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.body(any(LinkedMultiValueMap.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomTokenApiResponse.class)).thenReturn(null);

            assertThatThrownBy(() -> kiwoomApiClient.authenticate(account))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BROKER_AUTH_FAILED));
        }

        @Test
        @DisplayName("API 호출 중 예외 발생 시 BROKER_AUTH_FAILED 예외")
        void authenticateApiException() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(tokenCache.getToken(1L)).thenReturn(null);
            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getTokenPath()).thenReturn("/oauth2/token");
            when(brokerRestClient.post()).thenThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> kiwoomApiClient.authenticate(account))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BROKER_AUTH_FAILED));
        }
    }

    @Nested
    @DisplayName("getBalance() 테스트")
    class GetBalanceTests {

        @Test
        @DisplayName("정상 잔고 조회 - 보유종목 매핑 및 현재가 0 처리")
        void getBalanceSuccess() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");
            when(account.getAccountNumber()).thenReturn("50123456-01");
            when(account.getBrokerType()).thenReturn(BrokerType.KIWOOM);

            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getBalancePath()).thenReturn("/api/dostk/acnt");

            KiwoomBalanceItem item = new KiwoomBalanceItem(
                    "005930", "삼성전자", "100", "72000.00",
                    "7550000", "350000", "4.86");
            KiwoomBalanceSummary summary = new KiwoomBalanceSummary("15230000", "5000000");
            KiwoomBalanceApiResponse apiResponse = new KiwoomBalanceApiResponse(
                    "0", "정상", "", List.of(item), List.of(summary));

            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.get()).thenReturn(getSpec);
            when(getSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
            when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomBalanceApiResponse.class)).thenReturn(apiResponse);

            BalanceResponse result = kiwoomApiClient.getBalance(account, "access-token");

            assertThat(result.holdings()).hasSize(1);
            assertThat(result.holdings().getFirst().stockCode()).isEqualTo("005930");
            assertThat(result.holdings().getFirst().stockName()).isEqualTo("삼성전자");
            assertThat(result.holdings().getFirst().quantity()).isEqualTo(100L);
            // 키움 잔고에는 현재가 없으므로 0
            assertThat(result.holdings().getFirst().currentPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalEvaluationAmount()).isEqualByComparingTo(new BigDecimal("15230000"));
        }

        @Test
        @DisplayName("보유수량 0인 종목은 제외")
        void getBalanceFiltersZeroQuantity() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");
            when(account.getAccountNumber()).thenReturn("50123456-01");
            when(account.getBrokerType()).thenReturn(BrokerType.KIWOOM);

            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getBalancePath()).thenReturn("/api/dostk/acnt");

            KiwoomBalanceItem zeroItem = new KiwoomBalanceItem(
                    "000660", "SK하이닉스", "0", "150000.00",
                    "0", "0", "0");
            KiwoomBalanceApiResponse apiResponse = new KiwoomBalanceApiResponse(
                    "0", "정상", "", List.of(zeroItem), List.of());

            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.get()).thenReturn(getSpec);
            when(getSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
            when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomBalanceApiResponse.class)).thenReturn(apiResponse);

            BalanceResponse result = kiwoomApiClient.getBalance(account, "access-token");

            assertThat(result.holdings()).isEmpty();
        }

        @Test
        @DisplayName("API 응답이 null이면 BROKER_API_ERROR 예외")
        void getBalanceNullResponse() {
            Account account = mock(Account.class);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");
            when(account.getAccountNumber()).thenReturn("50123456-01");

            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getBalancePath()).thenReturn("/api/dostk/acnt");

            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.get()).thenReturn(getSpec);
            when(getSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
            when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomBalanceApiResponse.class)).thenReturn(null);

            assertThatThrownBy(() -> kiwoomApiClient.getBalance(account, "access-token"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BROKER_API_ERROR));
        }
    }

    @Nested
    @DisplayName("getCurrentPrice() 테스트")
    class GetCurrentPriceTests {

        @Test
        @DisplayName("정상 시세 조회")
        void getCurrentPriceSuccess() {
            Account account = mock(Account.class);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getPricePath()).thenReturn("/api/dostk/mrkt");

            KiwoomPriceOutput output = new KiwoomPriceOutput(
                    "005930", "삼성전자", "75500", "1500", "2.03",
                    "12345678", "76000", "74000", "74500");
            KiwoomPriceApiResponse apiResponse = new KiwoomPriceApiResponse("0", "정상", "", output);

            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.get()).thenReturn(getSpec);
            when(getSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
            when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomPriceApiResponse.class)).thenReturn(apiResponse);

            StockPriceResponse result = kiwoomApiClient.getCurrentPrice(account, "access-token", "005930");

            assertThat(result.stockCode()).isEqualTo("005930");
            assertThat(result.stockName()).isEqualTo("삼성전자");
            assertThat(result.currentPrice()).isEqualByComparingTo(new BigDecimal("75500"));
            assertThat(result.changePrice()).isEqualByComparingTo(new BigDecimal("1500"));
            assertThat(result.changeRate()).isEqualByComparingTo(new BigDecimal("2.03"));
            assertThat(result.volume()).isEqualTo(12345678L);
        }

        @Test
        @DisplayName("API 응답이 null이면 BROKER_API_ERROR 예외")
        void getCurrentPriceNullResponse() {
            Account account = mock(Account.class);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getPricePath()).thenReturn("/api/dostk/mrkt");

            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.get()).thenReturn(getSpec);
            when(getSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
            when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomPriceApiResponse.class)).thenReturn(null);

            assertThatThrownBy(() -> kiwoomApiClient.getCurrentPrice(account, "token", "005930"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BROKER_API_ERROR));
        }

        @Test
        @DisplayName("API 호출 중 예외 발생 시 BROKER_API_ERROR 예외")
        void getCurrentPriceApiException() {
            Account account = mock(Account.class);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://openapi.kiwoom.com");
            when(kiwoomProperties.getPricePath()).thenReturn("/api/dostk/mrkt");
            when(brokerRestClient.get()).thenThrow(new RuntimeException("Timeout"));

            assertThatThrownBy(() -> kiwoomApiClient.getCurrentPrice(account, "token", "005930"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BROKER_API_ERROR));
        }
    }
}
