package com.stock.account.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisBalanceSummary(
        @JsonProperty("dnca_tot_amt") String dncaTotAmt,
        @JsonProperty("scts_evlu_amt") String sctsEvluAmt,
        @JsonProperty("tot_evlu_amt") String totEvluAmt,
        @JsonProperty("nass_amt") String nassAmt,
        @JsonProperty("pchs_amt_smtl_amt") String pchsAmtSmtlAmt,
        @JsonProperty("evlu_pfls_smtl_amt") String evluPflsSmtlAmt
) {
}
