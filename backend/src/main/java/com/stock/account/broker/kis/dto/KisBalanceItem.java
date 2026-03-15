package com.stock.account.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisBalanceItem(
        @JsonProperty("pdno") String pdno,
        @JsonProperty("prdt_name") String prdtName,
        @JsonProperty("hldg_qty") String hldgQty,
        @JsonProperty("pchs_avg_pric") String pchsAvgPric,
        @JsonProperty("prpr") String prpr,
        @JsonProperty("evlu_amt") String evluAmt,
        @JsonProperty("evlu_pfls_amt") String evluPflsAmt,
        @JsonProperty("evlu_pfls_rt") String evluPflsRt
) {
}
