package com.stock.account.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KisBalanceApiResponse(
        @JsonProperty("rt_cd") String rtCd,
        @JsonProperty("msg_cd") String msgCd,
        @JsonProperty("msg1") String msg1,
        @JsonProperty("output1") List<KisBalanceItem> output1,
        @JsonProperty("output2") List<KisBalanceSummary> output2,
        @JsonProperty("ctx_area_fk100") String ctxAreaFk100,
        @JsonProperty("ctx_area_nk100") String ctxAreaNk100
) {
}
