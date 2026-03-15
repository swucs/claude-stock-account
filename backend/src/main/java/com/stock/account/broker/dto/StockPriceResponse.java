package com.stock.account.broker.dto;

import java.math.BigDecimal;

public record StockPriceResponse(
        String stockCode,
        String stockName,
        BigDecimal currentPrice,
        BigDecimal changePrice,
        BigDecimal changeRate,
        long volume,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal openPrice
) {
}
