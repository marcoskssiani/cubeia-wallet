package com.cubeia.wallet.concurrency;

import com.cubeia.wallet.dto.AccountResponse;
import com.cubeia.wallet.dto.BalanceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Wallet — concurrency tests")
class ConcurrencyTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    private static final int THREAD_COUNT = 20;
    private static final long DEBIT_AMOUNT = 100L;

    @Test
    @DisplayName("20 concurrent debits of 100 on a 1000-cent account: exactly 10 succeed, balance = 0")
    void concurrentDebits_exactlyHalfSucceed_balanceNeverNegative() throws InterruptedException {
        String accountId = createAccount("concurrency-debit-" + UUID.randomUUID(), 1000L);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientCount = new AtomicInteger();
        AtomicInteger otherErrorCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ResponseEntity<String> response = postTransfer(
                        accountId, DEBIT_AMOUNT, "DEBIT", "Concurrent debit", null);
                    int status = response.getStatusCode().value();
                    if (status == HttpStatus.CREATED.value()) successCount.incrementAndGet();
                    else if (status == HttpStatus.UNPROCESSABLE_ENTITY.value()) insufficientCount.incrementAndGet();
                    else otherErrorCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(completed).as("All threads should complete within 30s").isTrue();
        assertThat(otherErrorCount.get()).as("No unexpected errors").isEqualTo(0);
        assertThat(successCount.get()).as("Exactly 10 debits succeed").isEqualTo(10);
        assertThat(insufficientCount.get()).as("Exactly 10 fail with INSUFFICIENT_FUNDS").isEqualTo(10);
        assertThat(getBalance(accountId)).as("Final balance is exactly 0").isEqualTo(0L);
    }

    @Test
    @DisplayName("20 concurrent credits all succeed, final balance = sum of credits")
    void concurrentCredits_allSucceed_correctFinalBalance() throws InterruptedException {
        String accountId = createAccount("concurrency-credit-" + UUID.randomUUID(), 0L);
        long creditAmount = 100L;

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ResponseEntity<String> resp = postTransfer(
                        accountId, creditAmount, "CREDIT", "Concurrent credit", null);
                    if (resp.getStatusCode().value() == HttpStatus.CREATED.value()) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();
        finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        assertThat(getBalance(accountId)).isEqualTo(THREAD_COUNT * creditAmount);
    }

    @Test
    @DisplayName("Same idempotencyKey from 10 concurrent threads creates exactly one transaction")
    void concurrentIdempotentRequests_createOnlyOneTransaction() throws InterruptedException {
        String accountId = createAccount("concurrency-idem-" + UUID.randomUUID(), 0L);
        String sharedKey = "shared-idem-key-" + UUID.randomUUID();

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threads);
        List<String> txIds = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ResponseEntity<String> resp = postTransfer(
                        accountId, 1000L, "CREDIT", "Idem deposit", sharedKey);
                    if (resp.getStatusCode().value() == HttpStatus.CREATED.value()) {
                        successCount.incrementAndGet();
                        try {
                            String txId = objectMapper.readTree(resp.getBody()).get("id").asText();
                            txIds.add(txId);
                        } catch (Exception ignored) {}
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGate.countDown();
        finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(threads);
        assertThat(txIds).containsOnly(txIds.get(0));
        assertThat(getBalance(accountId)).isEqualTo(1000L);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String createAccount(String externalRef, long initialBalance) {
        String body = String.format(
            "{\"externalReference\":\"%s\",\"currency\":\"EUR\",\"initialBalance\":%d}",
            externalRef, initialBalance);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<AccountResponse> resp = restTemplate.postForEntity(
            "/api/v1/accounts", new HttpEntity<>(body, headers), AccountResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private ResponseEntity<String> postTransfer(String accountId, long amount, String type,
                                                  String description, String idempotencyKey) {
        String idem = idempotencyKey != null ? ",\"idempotencyKey\":\"" + idempotencyKey + "\"" : "";
        String body = String.format(
            "{\"amount\":%d,\"type\":\"%s\",\"description\":\"%s\"%s}",
            amount, type, description, idem);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(
            "/api/v1/accounts/" + accountId + "/transfers",
            new HttpEntity<>(body, headers), String.class);
    }

    private long getBalance(String accountId) {
        return restTemplate.getForEntity(
            "/api/v1/accounts/" + accountId + "/balance", BalanceResponse.class)
            .getBody().balance();
    }
}
