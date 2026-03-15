package com.stock.account.broker;

import com.stock.account.broker.dto.BalanceResponse;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;

public interface BrokerApiClient {

    BrokerType getBrokerType();

    BrokerTokenResponse authenticate(Account account);

    BalanceResponse getBalance(Account account, String accessToken);

    StockPriceResponse getCurrentPrice(Account account, String accessToken, String stockCode);
}
