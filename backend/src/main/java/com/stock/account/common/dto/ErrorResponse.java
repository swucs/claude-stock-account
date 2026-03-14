package com.stock.account.common.dto;

public record ErrorResponse(
        String code,
        String message
) {
}
