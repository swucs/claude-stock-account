package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KiwoomTokenRequest(
        @JsonProperty("grant_type") String grantType,
        @JsonProperty("appkey") String appkey,
        @JsonProperty("secretkey") String secretkey
) {
    public static KiwoomTokenRequest of(String appKey, String secretKey) {
        return new KiwoomTokenRequest("client_credentials", appKey, secretKey);
    }
}
