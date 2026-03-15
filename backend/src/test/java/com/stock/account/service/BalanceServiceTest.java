package com.stock.account.service;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.BrokerApiClientFactory;
import com.stock.account.broker.dto.BalanceResponse;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockHolding;
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
class BalanceServiceTest {

    @Autowired
    private BalanceService balanceService;

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
                new SignupRequest("balance-test@example.com", "password123!", "잔고테스트"));
        userId = user.id();

        AccountResponse account = accountService.createAccount(userId, new AccountCreateRequest(
                BrokerType.KIS, "50123456-01", "한투 계좌",
                "PSxqabcd1234", "HgT2efgh5678", null));
        accountId = account.id();
    }

    @Test
    @DisplayName("잔고 조회 성공")
    void getBalanceSuccess() {
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);

        BrokerTokenResponse token = new BrokerTokenResponse("test-token", "Bearer", LocalDateTime.now().plusHours(24));
        when(mockClient.authenticate(any())).thenReturn(token);

        BalanceResponse expectedResponse = new BalanceResponse(
                accountId, "50123456-01", "한국투자증권",
                new BigDecimal("10000000"), new BigDecimal("9000000"), new BigDecimal("1000000"),
                List.of(new StockHolding("005930", "삼성전자", 10,
                        new BigDecimal("70000"), new BigDecimal("75000"),
                        new BigDecimal("750000"), new BigDecimal("50000"), new BigDecimal("7.14"))));
        when(mockClient.getBalance(any(), eq("test-token"))).thenReturn(expectedResponse);

        BalanceResponse result = balanceService.getBalance(accountId, userId);

        assertThat(result.accountId()).isEqualTo(accountId);
        assertThat(result.brokerName()).isEqualTo("한국투자증권");
        assertThat(result.holdings()).hasSize(1);
        assertThat(result.holdings().getFirst().stockName()).isEqualTo("삼성전자");
        assertThat(result.totalEvaluationAmount()).isEqualByComparingTo(new BigDecimal("10000000"));
    }

    @Test
    @DisplayName("존재하지 않는 계좌 조회 시 ACCOUNT_NOT_FOUND")
    void getBalanceAccountNotFound() {
        assertThatThrownBy(() -> balanceService.getBalance(999L, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("타인의 계좌 조회 시 ACCOUNT_ACCESS_DENIED")
    void getBalanceAccessDenied() {
        UserResponse otherUser = userService.signup(
                new SignupRequest("other-balance@example.com", "password123!", "다른사용자"));

        assertThatThrownBy(() -> balanceService.getBalance(accountId, otherUser.id()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("증권사 인증 실패 시 예외 전파")
    void getBalanceAuthFailed() {
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);
        when(mockClient.authenticate(any())).thenThrow(new BusinessException(ErrorCode.BROKER_AUTH_FAILED));

        assertThatThrownBy(() -> balanceService.getBalance(accountId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BROKER_AUTH_FAILED));
    }
}
