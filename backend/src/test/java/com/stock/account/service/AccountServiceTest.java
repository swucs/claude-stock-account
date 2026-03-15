package com.stock.account.service;

import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.domain.BrokerType;
import com.stock.account.dto.*;
import com.stock.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountRepository accountRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        UserResponse user = userService.signup(
                new SignupRequest("account-test@example.com", "password123!", "테스트사용자"));
        userId = user.id();
    }

    private AccountCreateRequest createRequest() {
        return new AccountCreateRequest(
                BrokerType.KIS,
                "50123456-01",
                "한투 계좌",
                "PSxqabcd1234",
                "HgT2efgh5678",
                null
        );
    }

    @Test
    @DisplayName("계좌 등록 성공")
    void createAccountSuccess() {
        AccountResponse response = accountService.createAccount(userId, createRequest());

        assertThat(response.id()).isNotNull();
        assertThat(response.brokerType()).isEqualTo(BrokerType.KIS);
        assertThat(response.brokerName()).isEqualTo("한국투자증권");
        assertThat(response.accountName()).isEqualTo("한투 계좌");
        // 마스킹 확인
        assertThat(response.appKey()).isEqualTo("PSxq****");
        assertThat(response.secretKey()).isEqualTo("HgT2****");
        assertThat(response.accountNumber()).isEqualTo("50123456-01");
    }

    @Test
    @DisplayName("계좌 등록 실패 - 존재하지 않는 사용자")
    void createAccountUserNotFound() {
        assertThatThrownBy(() -> accountService.createAccount(999L, createRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("계좌 목록 조회 - 전체")
    void getAccountsAll() {
        accountService.createAccount(userId, createRequest());
        accountService.createAccount(userId, new AccountCreateRequest(
                BrokerType.KIWOOM, "70001234-01", "키움 계좌",
                "KiwoomKey1234", "KiwoomSecret56", null));

        List<AccountResponse> accounts = accountService.getAccounts(userId, null);

        assertThat(accounts).hasSize(2);
    }

    @Test
    @DisplayName("계좌 목록 조회 - 증권사별 필터")
    void getAccountsByBrokerType() {
        accountService.createAccount(userId, createRequest());
        accountService.createAccount(userId, new AccountCreateRequest(
                BrokerType.KIWOOM, "70001234-01", "키움 계좌",
                "KiwoomKey1234", "KiwoomSecret56", null));

        List<AccountResponse> kisAccounts = accountService.getAccounts(userId, BrokerType.KIS);
        List<AccountResponse> kiwoomAccounts = accountService.getAccounts(userId, BrokerType.KIWOOM);

        assertThat(kisAccounts).hasSize(1);
        assertThat(kiwoomAccounts).hasSize(1);
        assertThat(kisAccounts.get(0).brokerType()).isEqualTo(BrokerType.KIS);
    }

    @Test
    @DisplayName("계좌 상세 조회 성공")
    void getAccountSuccess() {
        AccountResponse created = accountService.createAccount(userId, createRequest());

        AccountResponse response = accountService.getAccount(created.id(), userId);

        assertThat(response.id()).isEqualTo(created.id());
        assertThat(response.brokerType()).isEqualTo(BrokerType.KIS);
    }

    @Test
    @DisplayName("계좌 상세 조회 실패 - 존재하지 않는 계좌")
    void getAccountNotFound() {
        assertThatThrownBy(() -> accountService.getAccount(999L, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("계좌 상세 조회 실패 - 타인의 계좌 접근")
    void getAccountAccessDenied() {
        AccountResponse created = accountService.createAccount(userId, createRequest());

        // 다른 사용자 생성
        UserResponse otherUser = userService.signup(
                new SignupRequest("other@example.com", "password123!", "다른사용자"));

        assertThatThrownBy(() -> accountService.getAccount(created.id(), otherUser.id()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("계좌 수정 성공")
    void updateAccountSuccess() {
        AccountResponse created = accountService.createAccount(userId, createRequest());

        AccountUpdateRequest updateRequest = new AccountUpdateRequest(
                null, "수정된 계좌명", "NewAppKey1234", null, null);

        AccountResponse response = accountService.updateAccount(created.id(), userId, updateRequest);

        assertThat(response.accountName()).isEqualTo("수정된 계좌명");
        assertThat(response.appKey()).isEqualTo("NewA****");
    }

    @Test
    @DisplayName("계좌 수정 실패 - 타인의 계좌")
    void updateAccountAccessDenied() {
        AccountResponse created = accountService.createAccount(userId, createRequest());

        UserResponse otherUser = userService.signup(
                new SignupRequest("other2@example.com", "password123!", "다른사용자2"));

        AccountUpdateRequest updateRequest = new AccountUpdateRequest(null, "해킹", null, null, null);

        assertThatThrownBy(() -> accountService.updateAccount(created.id(), otherUser.id(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("계좌 삭제 성공")
    void deleteAccountSuccess() {
        AccountResponse created = accountService.createAccount(userId, createRequest());

        accountService.deleteAccount(created.id(), userId);

        assertThat(accountRepository.findByIdAndUserId(created.id(), userId)).isEmpty();
    }

    @Test
    @DisplayName("계좌 삭제 실패 - 타인의 계좌")
    void deleteAccountAccessDenied() {
        AccountResponse created = accountService.createAccount(userId, createRequest());

        UserResponse otherUser = userService.signup(
                new SignupRequest("other3@example.com", "password123!", "다른사용자3"));

        assertThatThrownBy(() -> accountService.deleteAccount(created.id(), otherUser.id()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("다른 사용자의 계좌는 목록에 표시되지 않음")
    void getAccountsIsolation() {
        accountService.createAccount(userId, createRequest());

        UserResponse otherUser = userService.signup(
                new SignupRequest("other4@example.com", "password123!", "다른사용자4"));

        List<AccountResponse> otherAccounts = accountService.getAccounts(otherUser.id(), null);

        assertThat(otherAccounts).isEmpty();
    }
}
