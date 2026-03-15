package com.stock.account.controller;

import com.stock.account.common.dto.ApiResponse;
import com.stock.account.domain.BrokerType;
import com.stock.account.dto.AccountCreateRequest;
import com.stock.account.dto.AccountResponse;
import com.stock.account.dto.AccountUpdateRequest;
import com.stock.account.dto.BrokerInfoResponse;
import com.stock.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Account", description = "계좌 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 등록")
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            Authentication authentication,
            @Valid @RequestBody AccountCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        AccountResponse response = accountService.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(summary = "계좌 목록 조회")
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts(
            Authentication authentication,
            @RequestParam(required = false) BrokerType brokerType) {
        Long userId = (Long) authentication.getPrincipal();
        List<AccountResponse> response = accountService.getAccounts(userId, brokerType);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "계좌 상세 조회")
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            Authentication authentication,
            @PathVariable Long accountId) {
        Long userId = (Long) authentication.getPrincipal();
        AccountResponse response = accountService.getAccount(accountId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "계좌 수정")
    @PutMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            Authentication authentication,
            @PathVariable Long accountId,
            @Valid @RequestBody AccountUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        AccountResponse response = accountService.updateAccount(accountId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "계좌 삭제")
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            Authentication authentication,
            @PathVariable Long accountId) {
        Long userId = (Long) authentication.getPrincipal();
        accountService.deleteAccount(accountId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "지원 증권사 목록")
    @GetMapping("/brokers")
    public ResponseEntity<ApiResponse<List<BrokerInfoResponse>>> getBrokers() {
        List<BrokerInfoResponse> response = Arrays.stream(BrokerType.values())
                .map(BrokerInfoResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
