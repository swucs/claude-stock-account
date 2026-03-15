package com.stock.account.broker.kis;

import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;

/**
 * KIS API에서 사용하는 계좌번호 파싱 유틸리티.
 * 지원 형식:
 *   - "50123456-01" (하이픈 포함 8자리-2자리)
 *   - "5012345601"  (하이픈 없이 10자리)
 *   - "50123456"    (8자리만 → ACNT_PRDT_CD 기본값 "01")
 */
public final class KisAccountUtil {

    private KisAccountUtil() {
    }

    /**
     * 계좌번호에서 앞 8자리 (CANO)를 추출한다.
     */
    public static String parseCano(String accountNumber) {
        String normalized = normalize(accountNumber);
        return normalized.substring(0, 8);
    }

    /**
     * 계좌번호에서 뒤 2자리 (ACNT_PRDT_CD)를 추출한다.
     */
    public static String parseAcntPrdtCd(String accountNumber) {
        String normalized = normalize(accountNumber);
        if (normalized.length() >= 10) {
            return normalized.substring(8, 10);
        }
        return "01"; // 기본값
    }

    /**
     * 계좌번호에서 하이픈을 제거하고 숫자만 추출한다.
     */
    private static String normalize(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_FORMAT,
                    "계좌번호가 비어있습니다.");
        }

        String digits = accountNumber.replaceAll("[^0-9]", "");

        if (digits.length() < 8) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_FORMAT,
                    "KIS 계좌번호는 최소 8자리 숫자여야 합니다. 입력값: " + accountNumber);
        }

        return digits;
    }
}
