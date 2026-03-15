package com.stock.account.service;

import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.dto.*;
import com.stock.account.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("회원가입 성공")
    void signupSuccess() {
        SignupRequest request = new SignupRequest("test@example.com", "password123!", "홍길동");

        UserResponse response = userService.signup(request);

        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.id()).isNotNull();
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signupDuplicateEmail() {
        SignupRequest request = new SignupRequest("test@example.com", "password123!", "홍길동");
        userService.signup(request);

        SignupRequest duplicateRequest = new SignupRequest("test@example.com", "otherPw123!", "김철수");

        assertThatThrownBy(() -> userService.signup(duplicateRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("로그인 성공")
    void loginSuccess() {
        userService.signup(new SignupRequest("test@example.com", "password123!", "홍길동"));

        LoginRequest request = new LoginRequest("test@example.com", "password123!");

        TokenResponse response = userService.login(request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isPositive();
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 없음")
    void loginEmailNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password");

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void loginWrongPassword() {
        userService.signup(new SignupRequest("test@example.com", "password123!", "홍길동"));

        LoginRequest request = new LoginRequest("test@example.com", "wrong");

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("사용자 조회 성공")
    void getUserSuccess() {
        UserResponse signupResponse = userService.signup(
                new SignupRequest("test@example.com", "password123!", "홍길동"));

        UserResponse response = userService.getUser(signupResponse.id());

        assertThat(response.id()).isEqualTo(signupResponse.id());
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("사용자 조회 실패 - 존재하지 않는 사용자")
    void getUserNotFound() {
        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("사용자 정보 수정 - 이름만 변경")
    void updateUserNameOnly() {
        UserResponse signupResponse = userService.signup(
                new SignupRequest("test@example.com", "password123!", "홍길동"));

        UserUpdateRequest request = new UserUpdateRequest("김철수", null, null);

        UserResponse response = userService.updateUser(signupResponse.id(), request);

        assertThat(response.name()).isEqualTo("김철수");
    }

    @Test
    @DisplayName("사용자 정보 수정 - 비밀번호 변경")
    void updateUserPassword() {
        UserResponse signupResponse = userService.signup(
                new SignupRequest("test@example.com", "password123!", "홍길동"));

        UserUpdateRequest request = new UserUpdateRequest(null, "password123!", "newPassword1!");

        UserResponse response = userService.updateUser(signupResponse.id(), request);

        assertThat(response).isNotNull();
        // 변경된 비밀번호로 로그인 성공 확인
        TokenResponse loginResponse = userService.login(
                new LoginRequest("test@example.com", "newPassword1!"));
        assertThat(loginResponse.accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 미입력")
    void updatePasswordWithoutCurrentPassword() {
        UserResponse signupResponse = userService.signup(
                new SignupRequest("test@example.com", "password123!", "홍길동"));

        UserUpdateRequest request = new UserUpdateRequest(null, null, "newPassword1!");

        assertThatThrownBy(() -> userService.updateUser(signupResponse.id(), request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PASSWORD));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updatePasswordWrongCurrentPassword() {
        UserResponse signupResponse = userService.signup(
                new SignupRequest("test@example.com", "password123!", "홍길동"));

        UserUpdateRequest request = new UserUpdateRequest(null, "wrongPassword", "newPassword1!");

        assertThatThrownBy(() -> userService.updateUser(signupResponse.id(), request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PASSWORD));
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshSuccess() {
        userService.signup(new SignupRequest("test@example.com", "password123!", "홍길동"));
        TokenResponse loginResponse = userService.login(
                new LoginRequest("test@example.com", "password123!"));

        RefreshTokenRequest request = new RefreshTokenRequest(loginResponse.refreshToken());

        TokenResponse response = userService.refresh(request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }
}
