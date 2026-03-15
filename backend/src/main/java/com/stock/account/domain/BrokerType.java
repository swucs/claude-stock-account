package com.stock.account.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BrokerType {

    KIS("한국투자증권"),
    KIWOOM("키움증권"),
    LS("LS증권");

    private final String displayName;
}
