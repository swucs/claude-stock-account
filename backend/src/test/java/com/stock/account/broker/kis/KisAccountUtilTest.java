package com.stock.account.broker.kis;

import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisAccountUtilTest {

    @Test
    @DisplayName("하이픈 포함 계좌번호에서 CANO 추출")
    void parseCanoWithHyphen() {
        assertThat(KisAccountUtil.parseCano("50123456-01")).isEqualTo("50123456");
    }

    @Test
    @DisplayName("하이픈 포함 계좌번호에서 ACNT_PRDT_CD 추출")
    void parseAcntPrdtCdWithHyphen() {
        assertThat(KisAccountUtil.parseAcntPrdtCd("50123456-01")).isEqualTo("01");
    }

    @Test
    @DisplayName("하이픈 없는 10자리 계좌번호에서 CANO 추출")
    void parseCanoWithoutHyphen() {
        assertThat(KisAccountUtil.parseCano("5012345601")).isEqualTo("50123456");
    }

    @Test
    @DisplayName("하이픈 없는 10자리 계좌번호에서 ACNT_PRDT_CD 추출")
    void parseAcntPrdtCdWithoutHyphen() {
        assertThat(KisAccountUtil.parseAcntPrdtCd("5012345601")).isEqualTo("01");
    }

    @Test
    @DisplayName("8자리만 입력 시 CANO 추출")
    void parseCanoEightDigits() {
        assertThat(KisAccountUtil.parseCano("64035496")).isEqualTo("64035496");
    }

    @Test
    @DisplayName("8자리만 입력 시 ACNT_PRDT_CD 기본값 01")
    void parseAcntPrdtCdDefault() {
        assertThat(KisAccountUtil.parseAcntPrdtCd("64035496")).isEqualTo("01");
    }

    @Test
    @DisplayName("null 계좌번호 - 예외 발생")
    void parseNullAccountNumber() {
        assertThatThrownBy(() -> KisAccountUtil.parseCano(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_ACCOUNT_FORMAT));
    }

    @Test
    @DisplayName("7자리 이하 계좌번호 - 예외 발생")
    void parseTooShortAccountNumber() {
        assertThatThrownBy(() -> KisAccountUtil.parseCano("1234567"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_ACCOUNT_FORMAT));
    }

    @Test
    @DisplayName("빈 문자열 - 예외 발생")
    void parseEmptyString() {
        assertThatThrownBy(() -> KisAccountUtil.parseCano(""))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_ACCOUNT_FORMAT));
    }
}
