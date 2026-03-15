package com.stock.account.dto;

import com.stock.account.domain.BrokerType;

public record BrokerInfoResponse(
        BrokerType code,
        String name
) {
    public static BrokerInfoResponse from(BrokerType brokerType) {
        return new BrokerInfoResponse(brokerType, brokerType.getDisplayName());
    }
}
