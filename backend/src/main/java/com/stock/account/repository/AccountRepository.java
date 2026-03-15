package com.stock.account.repository;

import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    List<Account> findByUserIdAndBrokerType(Long userId, BrokerType brokerType);

    Optional<Account> findByIdAndUserId(Long id, Long userId);
}
