package com.stock.account.broker;

import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.domain.BrokerType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BrokerApiClientFactory {

    private final Map<BrokerType, BrokerApiClient> clients;

    public BrokerApiClientFactory(List<BrokerApiClient> clientList) {
        this.clients = clientList.stream()
                .collect(Collectors.toMap(BrokerApiClient::getBrokerType, Function.identity()));
    }

    public BrokerApiClient getClient(BrokerType brokerType) {
        BrokerApiClient client = clients.get(brokerType);
        if (client == null) {
            throw new BusinessException(ErrorCode.BROKER_NOT_SUPPORTED);
        }
        return client;
    }
}
