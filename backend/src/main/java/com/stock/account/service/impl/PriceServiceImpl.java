package com.stock.account.service.impl;

import com.stock.account.broker.BrokerApiClient;
import com.stock.account.broker.BrokerApiClientFactory;
import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.broker.dto.StockPriceResponse;
import com.stock.account.common.exception.BusinessException;
import com.stock.account.common.exception.ErrorCode;
import com.stock.account.domain.Account;
import com.stock.account.repository.AccountRepository;
import com.stock.account.service.PriceService;
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
public class PriceServiceImpl implements PriceService {

    private final BrokerApiClientFactory clientFactory;
    private final AccountRepository accountRepository;

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5분
    private static final long PRICE_INTERVAL_SECONDS = 3;     // 3초 간격

    @Override
    public StockPriceResponse getCurrentPrice(Long accountId, Long userId, String stockCode) {
        Account account = findAccountWithOwnership(accountId, userId);
        BrokerApiClient client = clientFactory.getClient(account.getBrokerType());

        BrokerTokenResponse token = client.authenticate(account);
        return client.getCurrentPrice(account, token.accessToken(), stockCode);
    }

    @Override
    public SseEmitter streamPrices(Long accountId, Long userId, List<String> stockCodes) {
        Account account = findAccountWithOwnership(accountId, userId);
        BrokerApiClient client = clientFactory.getClient(account.getBrokerType());
        BrokerTokenResponse token = client.authenticate(account);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        // 2개 스레드: heartbeat 전용 + 시세 조회 전용
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        // Heartbeat: 2초마다 comment 이벤트 전송 — 프록시/브라우저 연결 유지
        ScheduledFuture<?> heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception ignored) {
                // 연결 종료 시 무시 (price 스레드에서 처리)
            }
        }, 1, 2, TimeUnit.SECONDS);

        // 시세 조회: 이전 사이클 완료 후 다음 사이클 예약 (API가 느릴 때 겹침 방지)
        scheduleNextPriceCycle(scheduler, emitter, client, account, token, stockCodes, accountId);

        // 정리 콜백
        Runnable cleanup = () -> {
            heartbeatFuture.cancel(true);
            scheduler.shutdown();
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    private void scheduleNextPriceCycle(
            ScheduledExecutorService scheduler,
            SseEmitter emitter,
            BrokerApiClient client,
            Account account,
            BrokerTokenResponse token,
            List<String> stockCodes,
            Long accountId) {

        scheduler.schedule(() -> {
            if (scheduler.isShutdown()) return;

            for (String stockCode : stockCodes) {
                if (scheduler.isShutdown()) return;
                try {
                    StockPriceResponse price = client.getCurrentPrice(account, token.accessToken(), stockCode);
                    emitter.send(SseEmitter.event().data(price));
                } catch (IOException e) {
                    log.warn("SSE 전송 실패 (클라이언트 연결 종료) - accountId: {}", accountId);
                    emitter.completeWithError(e);
                    return;
                } catch (Exception e) {
                    log.warn("시세 조회 실패 - accountId: {}, stockCode: {}, error: {}", accountId, stockCode, e.getMessage());
                }
            }

            // 이전 사이클 완료 후 다음 사이클 예약
            if (!scheduler.isShutdown()) {
                scheduleNextPriceCycle(scheduler, emitter, client, account, token, stockCodes, accountId);
            }
        }, PRICE_INTERVAL_SECONDS, TimeUnit.SECONDS);
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
