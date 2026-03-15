package com.stock.account.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisPriceOutput(
        @JsonProperty("stck_shrn_iscd") String stckShrnIscd,
        @JsonProperty("hts_kor_isnm") String htsKorIsnm,
        @JsonProperty("stck_prpr") String stckPrpr,
        @JsonProperty("prdy_vrss") String prdyVrss,
        @JsonProperty("prdy_ctrt") String prdyCtrt,
        @JsonProperty("acml_vol") String acmlVol,
        @JsonProperty("stck_hgpr") String stckHgpr,
        @JsonProperty("stck_lwpr") String stckLwpr,
        @JsonProperty("stck_oprc") String stckOprc
) {
}
