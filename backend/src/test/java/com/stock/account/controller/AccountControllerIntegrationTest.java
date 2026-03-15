package com.stock.account.controller;

import tools.jackson.databind.ObjectMapper;
import com.stock.account.domain.BrokerType;
import com.stock.account.dto.AccountCreateRequest;
import com.stock.account.dto.AccountUpdateRequest;
import com.stock.account.dto.LoginRequest;
import com.stock.account.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class AccountControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 회원가입 + 로그인
        SignupRequest signupRequest = new SignupRequest("acct-test@test.com", "password123!", "계좌테스트");
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("acct-test@test.com", "password123!");
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        accessToken = objectMapper.readTree(loginResponse)
                .path("data").path("accessToken").asText();
    }

    private AccountCreateRequest createRequest() {
        return new AccountCreateRequest(
                BrokerType.KIS, "50123456-01", "한투 계좌",
                "PSxqabcd1234", "HgT2efgh5678", null);
    }

    @Test
    @DisplayName("계좌 등록 성공")
    void createAccountSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.brokerType").value("KIS"))
                .andExpect(jsonPath("$.data.brokerName").value("한국투자증권"))
                .andExpect(jsonPath("$.data.accountName").value("한투 계좌"))
                .andExpect(jsonPath("$.data.appKey").value("PSxq****"))
                .andExpect(jsonPath("$.data.secretKey").value("HgT2****"))
                .andExpect(jsonPath("$.data.accountNumber").value("5012*****01"));
    }

    @Test
    @DisplayName("계좌 등록 실패 - 인증 없음")
    void createAccountUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("계좌 등록 실패 - 필수 필드 누락")
    void createAccountValidationFail() throws Exception {
        AccountCreateRequest invalidRequest = new AccountCreateRequest(
                null, "", null, "", "", null);

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("계좌 목록 조회 성공")
    void getAccountsSuccess() throws Exception {
        // 2개 계좌 등록
        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());

        AccountCreateRequest kiwoomRequest = new AccountCreateRequest(
                BrokerType.KIWOOM, "70001234-01", "키움 계좌",
                "KiwoomKey1234", "KiwoomSecret56", null);
        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kiwoomRequest)))
                .andExpect(status().isCreated());

        // 전체 조회
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 증권사별 필터
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("brokerType", "KIS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].brokerType").value("KIS"));
    }

    @Test
    @DisplayName("계좌 상세 조회 성공")
    void getAccountDetailSuccess() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long accountId = objectMapper.readTree(createResponse)
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(accountId))
                .andExpect(jsonPath("$.data.brokerType").value("KIS"));
    }

    @Test
    @DisplayName("계좌 수정 성공")
    void updateAccountSuccess() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long accountId = objectMapper.readTree(createResponse)
                .path("data").path("id").asLong();

        AccountUpdateRequest updateRequest = new AccountUpdateRequest(
                "수정된 계좌명", "NewAppKey1234", null, null);

        mockMvc.perform(put("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountName").value("수정된 계좌명"))
                .andExpect(jsonPath("$.data.appKey").value("NewA****"));
    }

    @Test
    @DisplayName("계좌 삭제 성공")
    void deleteAccountSuccess() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long accountId = objectMapper.readTree(createResponse)
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 삭제 후 조회 시 404
        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("지원 증권사 목록 조회")
    void getBrokersSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/brokers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].code").value("KIS"))
                .andExpect(jsonPath("$.data[0].name").value("한국투자증권"));
    }

    @Test
    @DisplayName("타인의 계좌 접근 시 403 반환")
    void accessOtherUserAccountForbidden() throws Exception {
        // 첫 번째 사용자로 계좌 생성
        String createResponse = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long accountId = objectMapper.readTree(createResponse)
                .path("data").path("id").asLong();

        // 두 번째 사용자 생성 + 로그인
        SignupRequest otherSignup = new SignupRequest("other@test.com", "password123!", "다른사용자");
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherSignup)))
                .andExpect(status().isCreated());

        LoginRequest otherLogin = new LoginRequest("other@test.com", "password123!");
        String otherLoginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String otherAccessToken = objectMapper.readTree(otherLoginResponse)
                .path("data").path("accessToken").asText();

        // 타인 계좌 접근 시도
        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AC002"));
    }
}
