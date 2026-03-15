package com.stock.account.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisPriceApiResponse(
        @JsonProperty("rt_cd") String rtCd,
        @JsonProperty("msg_cd") String msgCd,
        @JsonProperty("msg1") String msg1,
        @JsonProperty("output") KisPriceOutput output
) {
}
