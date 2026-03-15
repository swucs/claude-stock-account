package com.stock.account.repository;

import com.stock.account.domain.BrokerToken;
import com.stock.account.domain.BrokerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BrokerTokenRepository extends JpaRepository<BrokerToken, Long> {

    Optional<BrokerToken> findByAccountIdAndBrokerType(Long accountId, BrokerType brokerType);

    void deleteByAccountIdAndBrokerType(Long accountId, BrokerType brokerType);

    void deleteByAccountId(Long accountId);

    @Modifying
    @Query("DELETE FROM BrokerToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
