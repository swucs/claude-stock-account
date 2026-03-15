package com.stock.account.controller;

import com.stock.account.broker.dto.BalanceResponse;
import com.stock.account.common.dto.ApiResponse;
import com.stock.account.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Balance", description = "잔고 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @Operation(summary = "계좌 잔고 조회")
    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            Authentication authentication,
            @PathVariable Long accountId) {
        Long userId = (Long) authentication.getPrincipal();
        BalanceResponse response = balanceService.getBalance(accountId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
