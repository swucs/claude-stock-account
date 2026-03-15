package com.stock.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockAccountApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockAccountApplication.class, args);
    }
}
