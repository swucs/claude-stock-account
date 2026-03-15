package com.stock.account.broker.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KiwoomBalanceRequest(
        @JsonProperty("qry_tp") String qryTp,           // "1":합산, "2":개별
        @JsonProperty("dmst_stex_tp") String dmstStexTp // "KRX":한국거래소, "NXT":넥스트트레이드
) {
    public static KiwoomBalanceRequest defaultRequest() {
        return new KiwoomBalanceRequest("2", "KRX");
    }
}
