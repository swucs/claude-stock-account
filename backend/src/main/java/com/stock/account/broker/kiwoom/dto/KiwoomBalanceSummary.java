package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KiwoomBalanceSummary(
        @JsonProperty("tot_evlu_amt") String totEvluAmt,
        @JsonProperty("dnca_tot_amt") String dncaTotAmt
) {
}
