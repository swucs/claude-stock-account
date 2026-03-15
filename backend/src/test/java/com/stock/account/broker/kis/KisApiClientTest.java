package com.stock.account.broker.kis;

import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.broker.kis.dto.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KisApiClientTest {

    @Mock
    private RestClient brokerRestClient;

    @Mock
    private BrokerProperties brokerProperties;

    @Mock
    private AesEncryptionUtil aesEncryptionUtil;

    @Mock
    private KisTokenCache tokenCache;

    @Mock
    private BrokerProperties.KisProperties kisProperties;

    private KisApiClient kisApiClient;

    @BeforeEach
    void setUp() {
        kisApiClient = new KisApiClient(brokerRestClient, brokerProperties, aesEncryptionUtil, tokenCache);
    }

    @Test
    @DisplayName("getBrokerType() - KIS 반환")
    void getBrokerType() {
        assertThat(kisApiClient.getBrokerType()).isEqualTo(BrokerType.KIS);
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

            BrokerTokenResponse result = kisApiClient.authenticate(account);

            assertThat(result.accessToken()).isEqualTo("cached-token");
            verify(brokerRestClient, never()).post();
        }

        @Test
        @DisplayName("캐시에 토큰이 없으면 KIS API 호출하여 토큰 발급")
        void authenticateWithApiCall() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(tokenCache.getToken(1L)).thenReturn(null);
            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKis()).thenReturn(kisProperties);
            when(kisProperties.getBaseUrl()).thenReturn("https://openapi.koreainvestment.com:9443");
            when(kisProperties.getTokenPath()).thenReturn("/oauth2/tokenP");

            KisTokenApiResponse apiResponse = new KisTokenApiResponse(
                    "new-access-token", "Bearer", 86400, "2026-03-16 21:00:00");

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.body(any(KisTokenRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KisTokenApiResponse.class)).thenReturn(apiResponse);

            BrokerTokenResponse result = kisApiClient.authenticate(account);

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
            when(brokerProperties.getKis()).thenReturn(kisProperties);
            when(kisProperties.getBaseUrl()).thenReturn("https://openapi.koreainvestment.com:9443");
            when(kisProperties.getTokenPath()).thenReturn("/oauth2/tokenP");

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.body(any(KisTokenRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KisTokenApiResponse.class)).thenReturn(null);

            assertThatThrownBy(() -> kisApiClient.authenticate(account))
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
            when(brokerProperties.getKis()).thenReturn(kisProperties);
            when(kisProperties.getBaseUrl()).thenReturn("https://openapi.koreainvestment.com:9443");
            when(kisProperties.getTokenPath()).thenReturn("/oauth2/tokenP");
            when(brokerRestClient.post()).thenThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> kisApiClient.authenticate(account))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BROKER_AUTH_FAILED));
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
            when(brokerProperties.getKis()).thenReturn(kisProperties);
            when(kisProperties.getBaseUrl()).thenReturn("https://openapi.koreainvestment.com:9443");
            when(kisProperties.getPricePath()).thenReturn("/uapi/domestic-stock/v1/quotations/inquire-price");

            KisPriceOutput output = new KisPriceOutput(
                    "005930", "삼성전자", "75500", "1500", "2.03",
                    "12345678", "76000", "74000", "74500");
            KisPriceApiResponse apiResponse = new KisPriceApiResponse("0", "정상", "", output);

            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.get()).thenReturn(getSpec);
            when(getSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
            when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KisPriceApiResponse.class)).thenReturn(apiResponse);

            StockPriceResponse result = kisApiClient.getCurrentPrice(account, "access-token", "005930");

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
            when(brokerProperties.getKis()).thenReturn(kisProperties);
            when(kisProperties.getBaseUrl()).thenReturn("https://openapi.koreainvestment.com:9443");
            when(kisProperties.getPricePath()).thenReturn("/uapi/domestic-stock/v1/quotations/inquire-price");

            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.get()).thenReturn(getSpec);
            when(getSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
            when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KisPriceApiResponse.class)).thenReturn(null);

            assertThatThrownBy(() -> kisApiClient.getCurrentPrice(account, "token", "005930"))
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
            when(brokerProperties.getKis()).thenReturn(kisProperties);
            when(kisProperties.getBaseUrl()).thenReturn("https://openapi.koreainvestment.com:9443");
            when(kisProperties.getPricePath()).thenReturn("/uapi/domestic-stock/v1/quotations/inquire-price");
            when(brokerRestClient.get()).thenThrow(new RuntimeException("Timeout"));

            assertThatThrownBy(() -> kisApiClient.getCurrentPrice(account, "token", "005930"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BROKER_API_ERROR));
        }
    }
}
