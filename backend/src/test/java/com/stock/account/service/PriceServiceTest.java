package com.stock.account.service;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.BrokerApiClientFactory;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.domain.BrokerType;
import com.stock.account.dto.AccountCreateRequest;
import com.stock.account.dto.AccountResponse;
import com.stock.account.dto.SignupRequest;
import com.stock.account.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class PriceServiceTest {

    @Autowired
    private PriceService priceService;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @MockitoBean
    private BrokerApiClientFactory clientFactory;

    private Long userId;
    private Long accountId;

    @BeforeEach
    void setUp() {
        UserResponse user = userService.signup(
                new SignupRequest("price-test@example.com", "password123!", "시세테스트"));
        userId = user.id();

        AccountResponse account = accountService.createAccount(userId, new AccountCreateRequest(
                BrokerType.KIS, "50123456-01", "한투 계좌",
                "PSxqabcd1234", "HgT2efgh5678", null));
        accountId = account.id();
    }

    @Test
    @DisplayName("단일 종목 현재가 조회 성공")
    void getCurrentPriceSuccess() {
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);

        BrokerTokenResponse token = new BrokerTokenResponse("test-token", "Bearer", LocalDateTime.now().plusHours(24));
        when(mockClient.authenticate(any())).thenReturn(token);

        StockPriceResponse expectedPrice = new StockPriceResponse(
                "005930", "삼성전자",
                new BigDecimal("75500"), new BigDecimal("1500"), new BigDecimal("2.03"),
                12345678L,
                new BigDecimal("76000"), new BigDecimal("74000"), new BigDecimal("74500"));
        when(mockClient.getCurrentPrice(any(), eq("test-token"), eq("005930"))).thenReturn(expectedPrice);

        StockPriceResponse result = priceService.getCurrentPrice(accountId, userId, "005930");

        assertThat(result.stockCode()).isEqualTo("005930");
        assertThat(result.stockName()).isEqualTo("삼성전자");
        assertThat(result.currentPrice()).isEqualByComparingTo(new BigDecimal("75500"));
        assertThat(result.changePrice()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(result.volume()).isEqualTo(12345678L);
    }

    @Test
    @DisplayName("존재하지 않는 계좌로 시세 조회 시 ACCOUNT_NOT_FOUND")
    void getCurrentPriceAccountNotFound() {
        assertThatThrownBy(() -> priceService.getCurrentPrice(999L, userId, "005930"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("타인의 계좌로 시세 조회 시 ACCOUNT_ACCESS_DENIED")
    void getCurrentPriceAccessDenied() {
        UserResponse otherUser = userService.signup(
                new SignupRequest("other-price@example.com", "password123!", "다른사용자"));

        assertThatThrownBy(() -> priceService.getCurrentPrice(accountId, otherUser.id(), "005930"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("증권사 인증 실패 시 예외 전파")
    void getCurrentPriceAuthFailed() {
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);
        when(mockClient.authenticate(any())).thenThrow(new BusinessException(ErrorCode.BROKER_AUTH_FAILED));

        assertThatThrownBy(() -> priceService.getCurrentPrice(accountId, userId, "005930"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BROKER_AUTH_FAILED));
    }

    @Test
    @DisplayName("SSE 스트리밍 - SseEmitter 반환 확인")
    void streamPricesReturnsSseEmitter() {
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);

        BrokerTokenResponse token = new BrokerTokenResponse("test-token", "Bearer", LocalDateTime.now().plusHours(24));
        when(mockClient.authenticate(any())).thenReturn(token);

        SseEmitter emitter = priceService.streamPrices(accountId, userId, List.of("005930", "035420"));

        assertThat(emitter).isNotNull();

        // 스케줄러 정리
        emitter.complete();
    }

    @Test
    @DisplayName("SSE 스트리밍 - 존재하지 않는 계좌 접근 시 예외")
    void streamPricesAccountNotFound() {
        assertThatThrownBy(() -> priceService.streamPrices(999L, userId, List.of("005930")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("SSE 스트리밍 - 타인 계좌 접근 시 예외")
    void streamPricesAccessDenied() {
        UserResponse otherUser = userService.signup(
                new SignupRequest("other-price2@example.com", "password123!", "다른사용자2"));

        assertThatThrownBy(() -> priceService.streamPrices(accountId, otherUser.id(), List.of("005930")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }
}
