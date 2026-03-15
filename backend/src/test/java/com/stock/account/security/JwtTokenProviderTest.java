package com.stock.account.security;

import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", "test-jwt-secret-key-for-testing-only-must-be-long");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 1800000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Access Token 생성 및 userId 추출")
    void createAccessTokenAndGetUserId() {
        String token = jwtTokenProvider.createAccessToken(1L);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Refresh Token 생성 및 타입 확인")
    void createRefreshTokenAndCheckType() {
        String token = jwtTokenProvider.createRefreshToken(1L);

        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("Access Token은 refresh 타입이 아님")
    void accessTokenIsNotRefreshType() {
        String token = jwtTokenProvider.createAccessToken(1L);

        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateValidToken() {
        String token = jwtTokenProvider.createAccessToken(1L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰 검증 실패 - INVALID_TOKEN")
    void validateInvalidToken() {
        assertThatThrownBy(() -> jwtTokenProvider.validateToken("invalid.token.here"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패 - EXPIRED_TOKEN")
    void validateExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLived, "secret", "test-jwt-secret-key-for-testing-only-must-be-long");
        ReflectionTestUtils.setField(shortLived, "accessTokenExpiration", -1000L); // 이미 만료
        ReflectionTestUtils.setField(shortLived, "refreshTokenExpiration", 604800000L);
        shortLived.init();

        String token = shortLived.createAccessToken(1L);

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EXPIRED_TOKEN));
    }

    @Test
    @DisplayName("Access Token 만료 시간(초) 반환")
    void getAccessTokenExpirationSeconds() {
        assertThat(jwtTokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(1800L);
    }
}
