package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KiwoomTokenApiResponse(
        @JsonProperty("token") String token,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_dt") String expiresDt,   // "yyyyMMddHHmmss" 형식
        @JsonProperty("return_code") Integer returnCode,
        @JsonProperty("return_msg") String returnMsg
) {
}
