package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KiwoomBalanceApiResponse(
        @JsonProperty("tot_pur_amt") String totPurAmt,           // 총매입금액
        @JsonProperty("tot_evlt_amt") String totEvltAmt,         // 총평가금액
        @JsonProperty("tot_evlt_pl") String totEvltPl,           // 총평가손익
        @JsonProperty("tot_prft_rt") String totPrftRt,           // 총수익률(%)
        @JsonProperty("acnt_evlt_remn_indv_tot") List<KiwoomBalanceItem> holdings  // 계좌평가잔고 개별
) {
}
