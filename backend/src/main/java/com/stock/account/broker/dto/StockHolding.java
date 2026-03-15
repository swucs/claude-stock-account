package com.stock.account.broker.dto;

import java.math.BigDecimal;

public record StockHolding(
        String stockCode,
        String stockName,
        long quantity,
        BigDecimal avgPurchasePrice,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitLossAmount,
        BigDecimal profitLossRate
) {
}
