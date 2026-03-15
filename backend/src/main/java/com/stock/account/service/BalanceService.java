package com.stock.account.service;

import com.stock.account.broker.dto.BalanceResponse;

public interface BalanceService {

    BalanceResponse getBalance(Long accountId, Long userId);
}
