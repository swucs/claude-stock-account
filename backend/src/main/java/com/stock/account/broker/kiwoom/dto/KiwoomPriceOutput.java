package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KiwoomPriceOutput(
        @JsonProperty("stk_cd") String stkCd,         // 종목코드
        @JsonProperty("stk_nm") String stkNm,         // 종목명
        @JsonProperty("cur_prc") String curPrc,        // 현재가
        @JsonProperty("pred_pre") String predPre,      // 전일대비
        @JsonProperty("pred_pre_sig") String predPreSig, // 전일대비기호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
        @JsonProperty("flu_rt") String fluRt,          // 등락율(%)
        @JsonProperty("trde_qty") String trdeQty,      // 거래량
        @JsonProperty("sel_bid") String selBid,        // 매도호가
        @JsonProperty("buy_bid") String buyBid         // 매수호가
) {
}
