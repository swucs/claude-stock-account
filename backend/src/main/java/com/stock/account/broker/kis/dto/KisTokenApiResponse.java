package com.stock.account.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisTokenApiResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("access_token_token_expired") String accessTokenExpired
) {
}
