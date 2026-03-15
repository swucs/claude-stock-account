package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KiwoomPriceApiResponse(
        @JsonProperty("atn_stk_infr") List<KiwoomPriceOutput> items  // 관심종목정보 목록
) {
}
