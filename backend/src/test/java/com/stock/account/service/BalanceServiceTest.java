package com.stock.account.service;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.BrokerApiClientFactory;
import com.stock.account.broker.dto.BalanceResponse;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockHolding;
import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;
import com.stock.account.repository.AccountRepository;
import com.stock.account.service.impl.BalanceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BrokerApiClientFactory clientFactory;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BalanceServiceImpl balanceService;

    @Test
    @DisplayName("잔고 조회 성공")
    void getBalanceSuccess() {
        Account account = mock(Account.class);
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(account.getBrokerType()).thenReturn(BrokerType.KIS);
        when(accountRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(account));
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);

        BrokerTokenResponse token = new BrokerTokenResponse("token", "Bearer", LocalDateTime.now().plusHours(24));
        when(mockClient.authenticate(account)).thenReturn(token);

        BalanceResponse expectedResponse = new BalanceResponse(
                1L, "50123456-01", "한국투자증권",
                new BigDecimal("10000000"), new BigDecimal("9000000"), new BigDecimal("1000000"),
                List.of(new StockHolding("005930", "삼성전자", 10,
                        new BigDecimal("70000"), new BigDecimal("75000"),
                        new BigDecimal("750000"), new BigDecimal("50000"), new BigDecimal("7.14"))));
        when(mockClient.getBalance(account, "token")).thenReturn(expectedResponse);

        BalanceResponse result = balanceService.getBalance(1L, 100L);

        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.brokerName()).isEqualTo("한국투자증권");
        assertThat(result.holdings()).hasSize(1);
        assertThat(result.holdings().getFirst().stockName()).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("존재하지 않는 계좌 조회 시 ACCOUNT_NOT_FOUND")
    void getBalanceAccountNotFound() {
        when(accountRepository.findByIdAndUserId(999L, 100L)).thenReturn(Optional.empty());
        when(accountRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> balanceService.getBalance(999L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("타인의 계좌 조회 시 ACCOUNT_ACCESS_DENIED")
    void getBalanceAccessDenied() {
        when(accountRepository.findByIdAndUserId(1L, 200L)).thenReturn(Optional.empty());
        when(accountRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> balanceService.getBalance(1L, 200L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("증권사 인증 실패 시 예외 전파")
    void getBalanceAuthFailed() {
        Account account = mock(Account.class);
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(account.getBrokerType()).thenReturn(BrokerType.KIS);
        when(accountRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(account));
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);
        when(mockClient.authenticate(account)).thenThrow(new BusinessException(ErrorCode.BROKER_AUTH_FAILED));

        assertThatThrownBy(() -> balanceService.getBalance(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BROKER_AUTH_FAILED));
    }
}
