package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KiwoomBalanceItem(
        @JsonProperty("stk_cd") String stkCd,           // 종목번호 (예: "A005930")
        @JsonProperty("stk_nm") String stkNm,           // 종목명
        @JsonProperty("cur_prc") String curPrc,          // 현재가
        @JsonProperty("rmnd_qty") String rmndQty,        // 잔고수량
        @JsonProperty("trde_able_qty") String trdeAbleQty, // 매매가능수량
        @JsonProperty("pur_pric") String purPric,        // 매입가
        @JsonProperty("pur_amt") String purAmt,          // 매입금액
        @JsonProperty("evlt_amt") String evltAmt,        // 평가금액
        @JsonProperty("evltv_prft") String elvtvPrft,    // 평가손익
        @JsonProperty("prft_rt") String prftRt           // 수익률(%)
) {
}
