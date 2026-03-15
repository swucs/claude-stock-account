package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KiwoomBalanceApiResponse(
        @JsonProperty("rt_cd") String rtCd,
        @JsonProperty("msg_cd") String msgCd,
        @JsonProperty("msg1") String msg1,
        @JsonProperty("output1") List<KiwoomBalanceItem> output1,
        @JsonProperty("output2") List<KiwoomBalanceSummary> output2
) {
}
