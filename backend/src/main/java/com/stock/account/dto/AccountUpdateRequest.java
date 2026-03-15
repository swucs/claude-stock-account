package com.stock.account.dto;

import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
        @Size(max = 100, message = "계좌명은 100자 이하여야 합니다.")
        String accountName,

        String appKey,

        String secretKey,

        String additionalInfo
) {
}
