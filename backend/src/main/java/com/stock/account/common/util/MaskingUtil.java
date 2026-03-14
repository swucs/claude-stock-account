package com.stock.account.common.util;

public final class MaskingUtil {

    private MaskingUtil() {
    }

    public static String maskKey(String key) {
        if (key == null || key.length() <= 4) {
            return key;
        }
        return key.substring(0, 4) + "****";
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 6) {
            return accountNumber;
        }
        String prefix = accountNumber.substring(0, 4);
        String suffix = accountNumber.substring(accountNumber.length() - 2);
        String masked = "*".repeat(accountNumber.length() - 6);
        return prefix + masked + suffix;
    }
}
