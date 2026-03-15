package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KiwoomPriceApiResponse(
        @JsonProperty("atn_stk_infr") List<KiwoomPriceOutput> items,  // 관심종목정보 목록
        @JsonProperty("return_code") Integer returnCode,
        @JsonProperty("return_msg") String returnMsg
) {
}
