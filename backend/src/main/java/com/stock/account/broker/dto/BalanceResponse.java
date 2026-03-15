package com.stock.account.broker.dto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceResponse(
        Long accountId,
        String accountNumber,
        String brokerName,
        BigDecimal totalEvaluationAmount,
        BigDecimal totalPurchaseAmount,
        BigDecimal totalProfitLossAmount,
        List<StockHolding> holdings
) {
}
