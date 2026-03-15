package com.stock.account.controller;

import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.common.dto.ApiResponse;
import com.stock.account.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Price", description = "실시간 시세 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @Operation(summary = "단건 시세 조회")
    @GetMapping("/accounts/{accountId}/price")
    public ResponseEntity<ApiResponse<StockPriceResponse>> getCurrentPrice(
            Authentication authentication,
            @PathVariable Long accountId,
            @RequestParam String stockCode) {
        Long userId = (Long) authentication.getPrincipal();
        StockPriceResponse response = priceService.getCurrentPrice(accountId, userId, stockCode);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "실시간 시세 스트림 (SSE)")
    @GetMapping(value = "/accounts/{accountId}/price/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPrices(
            Authentication authentication,
            @PathVariable Long accountId,
            @RequestParam List<String> stockCodes) {
        Long userId = (Long) authentication.getPrincipal();
        return priceService.streamPrices(accountId, userId, stockCodes);
    }
}
