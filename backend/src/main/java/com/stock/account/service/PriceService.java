package com.stock.account.service;

import com.stock.account.broker.dto.StockPriceResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface PriceService {

    StockPriceResponse getCurrentPrice(Long accountId, Long userId, String stockCode);

    SseEmitter streamPrices(Long accountId, Long userId, List<String> stockCodes);
}
