package com.stock.account.dto;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        String currentPassword,

        @Size(min = 8, max = 100, message = "새 비밀번호는 8자 이상이어야 합니다.")
        String newPassword
) {
}