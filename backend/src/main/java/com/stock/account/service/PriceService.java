package com.stock.account.service;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.BrokerApiClientFactory;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.domain.Account;
import com.stock.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceService {

    private final BrokerApiClientFactory clientFactory;
    private final AccountRepository accountRepository;

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5분
    private static final long PRICE_INTERVAL_SECONDS = 3;     // 3초 간격

    public StockPriceResponse getCurrentPrice(Long accountId, Long userId, String stockCode) {
        Account account = findAccountWithOwnership(accountId, userId);
        BrokerApiClient client = clientFactory.getClient(account.getBrokerType());

        BrokerTokenResponse token = client.authenticate(account);
        return client.getCurrentPrice(account, token.accessToken(), stockCode);
    }

    public SseEmitter streamPrices(Long accountId, Long userId, List<String> stockCodes) {
        Account account = findAccountWithOwnership(accountId, userId);
        BrokerApiClient client = clientFactory.getClient(account.getBrokerType());
        BrokerTokenResponse token = client.authenticate(account);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            for (String stockCode : stockCodes) {
                try {
                    StockPriceResponse price = client.getCurrentPrice(account, token.accessToken(), stockCode);
                    emitter.send(SseEmitter.event().data(price));
                } catch (IOException e) {
                    log.warn("SSE 전송 실패 (클라이언트 연결 종료) - accountId: {}", accountId);
                    emitter.completeWithError(e);
                    return;
                } catch (Exception e) {
                    log.warn("시세 조회 실패 - stockCode: {}, error: {}", stockCode, e.getMessage());
                    // 개별 종목 실패 시 다음 종목 계속 처리
                }
            }
        }, 0, PRICE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 정리 콜백
        Runnable cleanup = () -> {
            future.cancel(true);
            scheduler.shutdown();
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    private Account findAccountWithOwnership(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> {
                    if (accountRepository.existsById(accountId)) {
                        return new BusinessException(ErrorCode.ACCOUNT_ACCESS_DENIED);
                    }
                    return new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
                });
    }
}
