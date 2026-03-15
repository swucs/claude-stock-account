package com.stock.account.service;

import com.stock.account.domain.BrokerType;
import com.stock.account.dto.AccountCreateRequest;
import com.stock.account.dto.AccountResponse;
import com.stock.account.dto.AccountUpdateRequest;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(Long userId, AccountCreateRequest request);

    List<AccountResponse> getAccounts(Long userId, BrokerType brokerType);

    AccountResponse getAccount(Long accountId, Long userId);

    AccountResponse updateAccount(Long accountId, Long userId, AccountUpdateRequest request);

    void deleteAccount(Long accountId, Long userId);
}
