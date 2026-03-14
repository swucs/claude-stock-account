package com.stock.account.common.util;

import com.stock.account.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEncryptionUtilTest {

    private AesEncryptionUtil aesEncryptionUtil;

    @BeforeEach
    void setUp() {
        aesEncryptionUtil = new AesEncryptionUtil("test-secret-key-for-unit-tests");
    }

    @Test
    @DisplayName("암호화 후 복호화하면 원본과 일치해야 한다")
    void encryptAndDecrypt_shouldReturnOriginalText() {
        String original = "my-app-key-12345";

        String encrypted = aesEncryptionUtil.encrypt(original);
        String decrypted = aesEncryptionUtil.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("같은 텍스트를 2회 암호화하면 다른 암호문이 생성되어야 한다 (랜덤 IV)")
    void encrypt_samePlainText_shouldProduceDifferentCipherTexts() {
        String original = "same-text";

        String encrypted1 = aesEncryptionUtil.encrypt(original);
        String encrypted2 = aesEncryptionUtil.encrypt(original);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("암호문이 변조되면 복호화에 실패해야 한다")
    void decrypt_tamperedCipherText_shouldThrowException() {
        String original = "sensitive-data";
        String encrypted = aesEncryptionUtil.encrypt(original);

        // 암호문 변조
        char[] chars = encrypted.toCharArray();
        chars[chars.length - 1] = chars[chars.length - 1] == 'A' ? 'B' : 'A';
        String tampered = new String(chars);

        assertThatThrownBy(() -> aesEncryptionUtil.decrypt(tampered))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빈 문자열도 암호화/복호화가 정상 동작해야 한다")
    void encryptAndDecrypt_emptyString_shouldWork() {
        String original = "";

        String encrypted = aesEncryptionUtil.encrypt(original);
        String decrypted = aesEncryptionUtil.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("null 입력 시 암호화에서 예외가 발생해야 한다")
    void encrypt_null_shouldThrowException() {
        assertThatThrownBy(() -> aesEncryptionUtil.encrypt(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("null 입력 시 복호화에서 예외가 발생해야 한다")
    void decrypt_null_shouldThrowException() {
        assertThatThrownBy(() -> aesEncryptionUtil.decrypt(null))
                .isInstanceOf(BusinessException.class);
    }
}
