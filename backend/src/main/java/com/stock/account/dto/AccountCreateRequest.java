package com.stock.account.dto;

import com.stock.account.domain.BrokerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
        @NotNull(message = "증권사 유형은 필수입니다.")
        BrokerType brokerType,

        @NotBlank(message = "계좌번호는 필수입니다.")
        @Size(max = 50, message = "계좌번호는 50자 이하여야 합니다.")
        String accountNumber,

        @Size(max = 100, message = "계좌명은 100자 이하여야 합니다.")
        String accountName,

        @NotBlank(message = "App Key는 필수입니다.")
        String appKey,

        @NotBlank(message = "Secret Key는 필수입니다.")
        String secretKey,

        String additionalInfo
) {
}
