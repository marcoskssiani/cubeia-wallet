package com.cubeia.wallet.sse;

import com.cubeia.wallet.domain.Account;
import com.cubeia.wallet.domain.Transaction;
import com.cubeia.wallet.event.TransactionCreatedEvent;
import com.cubeia.wallet.repository.AccountRepository;
import com.cubeia.wallet.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ActivityFeedService {

    private static final Logger log = LoggerFactory.getLogger(ActivityFeedService.class);

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    // Set after ApplicationReadyEvent fires (i.e. after DataSeeder completes).
    // Null until then — sendRecentHistory returns nothing while the app is initialising.
    private volatile Instant liveFeedStart = null;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        liveFeedStart = Instant.now();
    }

    public ActivityFeedService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Opens an SSE connection and immediately replays the last 20 transactions
     * so a newly opened tab is not blank.
     */
    @Transactional(readOnly = true)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);

        sendRecentHistory(emitter);

        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }

    /**
     * Broadcasts after the database transaction commits.
     * @Async ensures this does not block the HTTP thread that committed the transaction.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        if (emitters.isEmpty()) return; // nobody listening — skip payload serialization entirely
        Map<String, Object> payload = buildPayload(
            event.transactionId(), event.accountId(), event.externalReference(),
            event.currency(), event.amount(), event.transactionType().name(),
            event.description(), event.postBalance(), event.timestamp().toString()
        );
        broadcast("transaction", payload);
    }

    @Scheduled(fixedDelay = 25_000)
    public void sendHeartbeat() {
        broadcast("heartbeat", Map.of("ts", Instant.now().toString()));
    }

    // -------------------------------------------------------------------------

    private void sendRecentHistory(SseEmitter emitter) {
        if (liveFeedStart == null) return;
        List<Transaction> chronological =
            transactionRepository.findByCreatedAtAfterOrderByCreatedAtAsc(liveFeedStart);

        for (Transaction tx : chronological) {
            Account account = accountRepository.findById(tx.getAccountId()).orElse(null);
            if (account == null) continue;

            Map<String, Object> payload = buildPayload(
                tx.getId(), tx.getAccountId(), account.getExternalReference(),
                account.getCurrency(), tx.getAmount(), tx.getType().name(),
                tx.getDescription(), tx.getPostBalance(),
                tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : Instant.now().toString()
            );
            try {
                emitter.send(SseEmitter.event().name("transaction").data(payload));
            } catch (IOException | IllegalStateException e) {
                // Client disconnected before history finished sending — stop early
                break;
            }
        }
    }

    private void broadcast(String eventName, Object data) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    private static Map<String, Object> buildPayload(
            String txId, String accountId, String externalRef, String currency,
            long amount, String txType, String description, long postBalance, String timestamp) {
        return Map.of(
            "type", "TRANSACTION",
            "transactionId", txId,
            "accountId", accountId,
            "externalReference", externalRef,
            "currency", currency,
            "amount", amount,
            "transactionType", txType,
            "description", description,
            "postBalance", postBalance,
            "timestamp", timestamp
        );
    }
}
