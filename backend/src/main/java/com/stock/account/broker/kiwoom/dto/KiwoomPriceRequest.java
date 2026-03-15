package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KiwoomPriceRequest(
        @JsonProperty("stk_cd") String stkCd  // 종목코드 (예: "A005930")
) {
}
