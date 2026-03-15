package com.stock.account.broker;

import com.stock.account.broker.dto.BalanceResponse;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrokerApiClientFactoryTest {

    @Test
    @DisplayName("등록된 클라이언트 반환")
    void getRegisteredClient() {
        BrokerApiClient mockClient = createMockClient(BrokerType.KIS);
        BrokerApiClientFactory factory = new BrokerApiClientFactory(List.of(mockClient));

        BrokerApiClient result = factory.getClient(BrokerType.KIS);

        assertThat(result.getBrokerType()).isEqualTo(BrokerType.KIS);
    }

    @Test
    @DisplayName("미등록 증권사 요청 시 예외")
    void getUnregisteredClient() {
        BrokerApiClient mockClient = createMockClient(BrokerType.KIS);
        BrokerApiClientFactory factory = new BrokerApiClientFactory(List.of(mockClient));

        assertThatThrownBy(() -> factory.getClient(BrokerType.KIWOOM))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BROKER_NOT_SUPPORTED));
    }

    private BrokerApiClient createMockClient(BrokerType type) {
        return new BrokerApiClient() {
            @Override
            public BrokerType getBrokerType() { return type; }
            @Override
            public BrokerTokenResponse authenticate(Account account) { return null; }
            @Override
            public BalanceResponse getBalance(Account account, String accessToken) { return null; }
            @Override
            public StockPriceResponse getCurrentPrice(Account account, String accessToken, String stockCode) { return null; }
        };
    }
}
