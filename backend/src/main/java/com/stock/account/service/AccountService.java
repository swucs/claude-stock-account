package com.stock.account.service;

import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.common.util.AesEncryptionUtil;
import com.stock.account.common.util.MaskingUtil;
import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;
import com.stock.account.domain.User;
import com.stock.account.dto.AccountCreateRequest;
import com.stock.account.dto.AccountResponse;
import com.stock.account.dto.AccountUpdateRequest;
import com.stock.account.repository.AccountRepository;
import com.stock.account.repository.BrokerTokenRepository;
import com.stock.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final BrokerTokenRepository brokerTokenRepository;
    private final AesEncryptionUtil aesEncryptionUtil;

    @Transactional
    public AccountResponse createAccount(Long userId, AccountCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Account account = Account.builder()
                .user(user)
                .brokerType(request.brokerType())
                .accountNumber(request.accountNumber())
                .accountName(request.accountName())
                .appKey(aesEncryptionUtil.encrypt(request.appKey()))
                .secretKey(aesEncryptionUtil.encrypt(request.secretKey()))
                .additionalInfo(request.additionalInfo())
                .build();

        Account saved = accountRepository.save(account);
        return toMaskedResponse(saved);
    }

    public List<AccountResponse> getAccounts(Long userId, BrokerType brokerType) {
        List<Account> accounts;
        if (brokerType != null) {
            accounts = accountRepository.findByUserIdAndBrokerType(userId, brokerType);
        } else {
            accounts = accountRepository.findByUserId(userId);
        }
        return accounts.stream()
                .map(this::toMaskedResponse)
                .toList();
    }

    public AccountResponse getAccount(Long accountId, Long userId) {
        Account account = findAccountWithOwnership(accountId, userId);
        return toMaskedResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long accountId, Long userId, AccountUpdateRequest request) {
        Account account = findAccountWithOwnership(accountId, userId);

        String encryptedAppKey = request.appKey() != null
                ? aesEncryptionUtil.encrypt(request.appKey()) : null;
        String encryptedSecretKey = request.secretKey() != null
                ? aesEncryptionUtil.encrypt(request.secretKey()) : null;

        account.update(request.accountNumber(), request.accountName(), encryptedAppKey, encryptedSecretKey, request.additionalInfo());

        return toMaskedResponse(account);
    }

    @Transactional
    public void deleteAccount(Long accountId, Long userId) {
        Account account = findAccountWithOwnership(accountId, userId);
        // 계좌 삭제 시 저장된 토큰도 함께 삭제
        brokerTokenRepository.deleteByAccountId(accountId);
        accountRepository.delete(account);
    }

    private Account findAccountWithOwnership(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> {
                    // 계좌 자체가 존재하는지 확인하여 적절한 에러 반환
                    if (accountRepository.existsById(accountId)) {
                        return new BusinessException(ErrorCode.ACCOUNT_ACCESS_DENIED);
                    }
                    return new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
                });
    }

    private AccountResponse toMaskedResponse(Account account) {
        String decryptedAppKey = aesEncryptionUtil.decrypt(account.getAppKey());
        String decryptedSecretKey = aesEncryptionUtil.decrypt(account.getSecretKey());

        return new AccountResponse(
                account.getId(),
                account.getBrokerType(),
                account.getBrokerType().getDisplayName(),
                account.getAccountNumber(),
                account.getAccountName(),
                MaskingUtil.maskKey(decryptedAppKey),
                MaskingUtil.maskKey(decryptedSecretKey),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
