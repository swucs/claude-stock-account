package com.stock.account.service;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.BrokerApiClientFactory;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;
import com.stock.account.repository.AccountRepository;
import com.stock.account.service.impl.PriceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private BrokerApiClientFactory clientFactory;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private PriceServiceImpl priceService;

    @Test
    @DisplayName("단일 종목 현재가 조회 성공")
    void getCurrentPriceSuccess() {
        Account account = mock(Account.class);
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(account.getBrokerType()).thenReturn(BrokerType.KIS);
        when(accountRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(account));
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);

        BrokerTokenResponse token = new BrokerTokenResponse("token", "Bearer", LocalDateTime.now().plusHours(24));
        when(mockClient.authenticate(account)).thenReturn(token);

        StockPriceResponse expectedPrice = new StockPriceResponse(
                "005930", "삼성전자",
                new BigDecimal("75500"), new BigDecimal("1500"), new BigDecimal("2.03"),
                12345678L,
                new BigDecimal("76000"), new BigDecimal("74000"), new BigDecimal("74500"));
        when(mockClient.getCurrentPrice(account, "token", "005930")).thenReturn(expectedPrice);

        StockPriceResponse result = priceService.getCurrentPrice(1L, 100L, "005930");

        assertThat(result.stockCode()).isEqualTo("005930");
        assertThat(result.stockName()).isEqualTo("삼성전자");
        assertThat(result.currentPrice()).isEqualByComparingTo(new BigDecimal("75500"));
    }

    @Test
    @DisplayName("존재하지 않는 계좌로 시세 조회 시 ACCOUNT_NOT_FOUND")
    void getCurrentPriceAccountNotFound() {
        when(accountRepository.findByIdAndUserId(999L, 100L)).thenReturn(Optional.empty());
        when(accountRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> priceService.getCurrentPrice(999L, 100L, "005930"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("타인의 계좌로 시세 조회 시 ACCOUNT_ACCESS_DENIED")
    void getCurrentPriceAccessDenied() {
        when(accountRepository.findByIdAndUserId(1L, 200L)).thenReturn(Optional.empty());
        when(accountRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> priceService.getCurrentPrice(1L, 200L, "005930"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("SSE 스트리밍 - SseEmitter 반환 확인")
    void streamPricesReturnsSseEmitter() {
        Account account = mock(Account.class);
        BrokerApiClient mockClient = mock(BrokerApiClient.class);
        when(account.getBrokerType()).thenReturn(BrokerType.KIS);
        when(accountRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(account));
        when(clientFactory.getClient(BrokerType.KIS)).thenReturn(mockClient);

        BrokerTokenResponse token = new BrokerTokenResponse("token", "Bearer", LocalDateTime.now().plusHours(24));
        when(mockClient.authenticate(account)).thenReturn(token);

        SseEmitter emitter = priceService.streamPrices(1L, 100L, List.of("005930", "035420"));

        assertThat(emitter).isNotNull();

        // 정리 (스케줄러 종료)
        emitter.complete();
    }

    @Test
    @DisplayName("SSE 스트리밍 - 타인 계좌 접근 시 예외")
    void streamPricesAccessDenied() {
        when(accountRepository.findByIdAndUserId(1L, 200L)).thenReturn(Optional.empty());
        when(accountRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> priceService.streamPrices(1L, 200L, List.of("005930")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }
}
