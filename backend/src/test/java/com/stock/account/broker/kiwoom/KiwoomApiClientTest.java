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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
                    "cached-token", "bearer", LocalDateTime.now().plusHours(12));
            when(tokenCache.getToken(1L)).thenReturn(cachedToken);

            BrokerTokenResponse result = kiwoomApiClient.authenticate(account);

            assertThat(result.accessToken()).isEqualTo("cached-token");
            verify(brokerRestClient, never()).post();
        }

        @Test
        @DisplayName("캐시 미스 시 키움 API 호출 (JSON body, appkey/secretkey 파라미터)")
        void authenticateWithApiCall() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(tokenCache.getToken(1L)).thenReturn(null);
            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
            when(kiwoomProperties.getTokenPath()).thenReturn("/oauth2/token");

            // expires_dt: "yyyyMMddHHmmss" 형식
            KiwoomTokenApiResponse apiResponse = new KiwoomTokenApiResponse(
                    "new-access-token", "bearer", "20261231235959", 0, "정상");

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.body(any(KiwoomTokenRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomTokenApiResponse.class)).thenReturn(apiResponse);

            BrokerTokenResponse result = kiwoomApiClient.authenticate(account);

            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.tokenType()).isEqualTo("bearer");
            assertThat(result.expiresAt()).isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
            verify(tokenCache).putToken(eq(1L), any(BrokerTokenResponse.class));
        }

        @Test
        @DisplayName("return_code != 0 이면 BROKER_AUTH_FAILED 예외")
        void authenticateReturnCodeError() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAppKey()).thenReturn("encrypted-appkey");
            when(account.getSecretKey()).thenReturn("encrypted-secret");

            when(tokenCache.getToken(1L)).thenReturn(null);
            when(aesEncryptionUtil.decrypt("encrypted-appkey")).thenReturn("plain-appkey");
            when(aesEncryptionUtil.decrypt("encrypted-secret")).thenReturn("plain-secret");
            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
            when(kiwoomProperties.getTokenPath()).thenReturn("/oauth2/token");

            KiwoomTokenApiResponse errorResponse = new KiwoomTokenApiResponse(
                    null, null, null, -1, "인증 오류");

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.body(any(KiwoomTokenRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomTokenApiResponse.class)).thenReturn(errorResponse);

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
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
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
        @DisplayName("정상 잔고 조회 - api-id: kt00018, A-prefix 제거, 현재가 포함")
        void getBalanceSuccess() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAccountNumber()).thenReturn("50123456-01");
            when(account.getBrokerType()).thenReturn(BrokerType.KIWOOM);

            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
            when(kiwoomProperties.getBalancePath()).thenReturn("/api/dostk/acnt");

            KiwoomBalanceItem item = new KiwoomBalanceItem(
                    "A005930", "삼성전자", "75500", "100", "100",
                    "72000", "7200000", "7550000", "350000", "4.86");
            KiwoomBalanceApiResponse apiResponse = new KiwoomBalanceApiResponse(
                    "14500000", "15230000", "730000", "5.03", List.of(item));

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            HttpHeaders headers = new HttpHeaders();
            // cont-yn 없음 → 단일 페이지
            ResponseEntity<KiwoomBalanceApiResponse> responseEntity =
                    ResponseEntity.ok().headers(headers).body(apiResponse);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
            when(bodySpec.body(any(KiwoomBalanceRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toEntity(KiwoomBalanceApiResponse.class)).thenReturn(responseEntity);

            BalanceResponse result = kiwoomApiClient.getBalance(account, "access-token");

            assertThat(result.holdings()).hasSize(1);
            assertThat(result.holdings().getFirst().stockCode()).isEqualTo("005930"); // A-prefix 제거
            assertThat(result.holdings().getFirst().stockName()).isEqualTo("삼성전자");
            assertThat(result.holdings().getFirst().quantity()).isEqualTo(100L);
            assertThat(result.holdings().getFirst().currentPrice()).isEqualByComparingTo(new BigDecimal("75500"));
            assertThat(result.totalEvaluationAmount()).isEqualByComparingTo(new BigDecimal("15230000"));
            assertThat(result.totalPurchaseAmount()).isEqualByComparingTo(new BigDecimal("14500000"));
        }

        @Test
        @DisplayName("보유수량 0인 종목은 제외")
        void getBalanceFiltersZeroQuantity() {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn(1L);
            when(account.getAccountNumber()).thenReturn("50123456-01");
            when(account.getBrokerType()).thenReturn(BrokerType.KIWOOM);

            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
            when(kiwoomProperties.getBalancePath()).thenReturn("/api/dostk/acnt");

            KiwoomBalanceItem zeroItem = new KiwoomBalanceItem(
                    "A000660", "SK하이닉스", "150000", "0", "0",
                    "150000", "0", "0", "0", "0");
            KiwoomBalanceApiResponse apiResponse = new KiwoomBalanceApiResponse(
                    "0", "0", "0", "0", List.of(zeroItem));

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            ResponseEntity<KiwoomBalanceApiResponse> responseEntity =
                    ResponseEntity.ok().body(apiResponse);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
            when(bodySpec.body(any(KiwoomBalanceRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toEntity(KiwoomBalanceApiResponse.class)).thenReturn(responseEntity);

            BalanceResponse result = kiwoomApiClient.getBalance(account, "access-token");
            assertThat(result.holdings()).isEmpty();
        }

        @Test
        @DisplayName("API 응답이 null이면 BROKER_API_ERROR 예외")
        void getBalanceNullResponse() {
            Account account = mock(Account.class);

            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
            when(kiwoomProperties.getBalancePath()).thenReturn("/api/dostk/acnt");

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            ResponseEntity<KiwoomBalanceApiResponse> responseEntity =
                    ResponseEntity.ok().body(null);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
            when(bodySpec.body(any(KiwoomBalanceRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toEntity(KiwoomBalanceApiResponse.class)).thenReturn(responseEntity);

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
        @DisplayName("정상 시세 조회 - api-id: ka10095, A-prefix 자동 추가")
        void getCurrentPriceSuccess() {
            Account account = mock(Account.class);

            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
            when(kiwoomProperties.getPricePath()).thenReturn("/api/dostk/stkinfo");

            KiwoomPriceOutput output = new KiwoomPriceOutput(
                    "A005930", "삼성전자", "75500", "1500", "2", "2.03", "12345678",
                    "75600", "75400");
            KiwoomPriceApiResponse apiResponse = new KiwoomPriceApiResponse(List.of(output));

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
            when(bodySpec.body(any(KiwoomPriceRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(KiwoomPriceApiResponse.class)).thenReturn(apiResponse);

            StockPriceResponse result = kiwoomApiClient.getCurrentPrice(account, "access-token", "005930");

            assertThat(result.stockCode()).isEqualTo("005930"); // 원래 stockCode 유지
            assertThat(result.stockName()).isEqualTo("삼성전자");
            assertThat(result.currentPrice()).isEqualByComparingTo(new BigDecimal("75500"));
            assertThat(result.changePrice()).isEqualByComparingTo(new BigDecimal("1500"));
            assertThat(result.changeRate()).isEqualByComparingTo(new BigDecimal("2.03"));
            assertThat(result.volume()).isEqualTo(12345678L);
        }

        @Test
        @DisplayName("응답 목록이 비어있으면 BROKER_API_ERROR 예외")
        void getCurrentPriceEmptyResponse() {
            Account account = mock(Account.class);

            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
            when(kiwoomProperties.getPricePath()).thenReturn("/api/dostk/stkinfo");

            RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(brokerRestClient.post()).thenReturn(postSpec);
            when(postSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any())).thenReturn(bodySpec);
            when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
            when(bodySpec.body(any(KiwoomPriceRequest.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
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

            when(brokerProperties.getKiwoom()).thenReturn(kiwoomProperties);
            when(kiwoomProperties.getBaseUrl()).thenReturn("https://api.kiwoom.com");
            when(kiwoomProperties.getPricePath()).thenReturn("/api/dostk/stkinfo");
            when(brokerRestClient.post()).thenThrow(new RuntimeException("Timeout"));

            assertThatThrownBy(() -> kiwoomApiClient.getCurrentPrice(account, "token", "005930"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BROKER_API_ERROR));
        }
    }
}
