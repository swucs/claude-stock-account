package com.stock.account.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "broker")
public class BrokerProperties {

    private KisProperties kis = new KisProperties();
    private BrokerEndpoint kiwoom = new BrokerEndpoint();
    private BrokerEndpoint ls = new BrokerEndpoint();

    @Getter
    @Setter
    public static class KisProperties {
        private String baseUrl;
        private String tokenPath;
        private String balancePath;
        private String pricePath;
    }

    @Getter
    @Setter
    public static class BrokerEndpoint {
        private String baseUrl;
        private String tokenPath;
        private String balancePath;
        private String pricePath;
    }
}
