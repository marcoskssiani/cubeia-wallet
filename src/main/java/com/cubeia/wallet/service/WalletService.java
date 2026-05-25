package com.cubeia.wallet.service;

import com.cubeia.wallet.domain.Account;
import com.cubeia.wallet.domain.Transaction;
import com.cubeia.wallet.dto.AccountResponse;
import com.cubeia.wallet.dto.BalanceResponse;
import com.cubeia.wallet.dto.CreateAccountRequest;
import com.cubeia.wallet.dto.StatsResponse;
import com.cubeia.wallet.dto.TransactionListResponse;
import com.cubeia.wallet.dto.TransactionResponse;
import com.cubeia.wallet.dto.TransferRequest;
import com.cubeia.wallet.exception.AccountNotFoundException;
import com.cubeia.wallet.exception.DuplicateAccountException;
import com.cubeia.wallet.exception.DuplicateIdempotencyKeyException;
import com.cubeia.wallet.exception.WalletException;
import com.cubeia.wallet.repository.AccountRepository;
import com.cubeia.wallet.repository.TransactionRepository;
import com.cubeia.wallet.sse.ActivityFeedService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    static final int MAX_RETRIES = 10;
    static final long RETRY_WAIT_MS = 50;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final WalletTransferService transferService;
    private final ActivityFeedService activityFeedService;

    public WalletService(AccountRepository accountRepository,
                         TransactionRepository transactionRepository,
                         WalletTransferService transferService,
                         ActivityFeedService activityFeedService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferService = transferService;
        this.activityFeedService = activityFeedService;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.findByExternalReference(request.externalReference()).isPresent()) {
            throw new DuplicateAccountException(request.externalReference());
        }
        Account account = new Account(
            UUID.randomUUID().toString(),
            request.externalReference(),
            request.currency(),
            request.resolvedInitialBalance()
        );
        return toAccountResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
        return toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
        return new BalanceResponse(account.getId(), account.getCurrency(), account.getBalance());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAllAccounts() {
        return accountRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toAccountResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        long accountCount = accountRepository.count();
        long totalBalance = accountRepository.sumAllBalances();
        long recentTxCount = transactionRepository.countByCreatedAtAfter(Instant.now().minusSeconds(300));
        int activeConnections = activityFeedService.getActiveConnectionCount();
        return new StatsResponse(accountCount, totalBalance, recentTxCount, activeConnections);
    }

    @Transactional(readOnly = true)
    public TransactionListResponse listTransactions(String accountId, int page, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        Page<Transaction> txPage = transactionRepository
            .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(page, size));

        List<TransactionResponse> responses = txPage.getContent().stream()
            .map(this::toTransactionResponse)
            .toList();

        return new TransactionListResponse(accountId, responses, txPage.getTotalElements(), page, size);
    }

    public TransactionResponse transfer(String accountId, TransferRequest request) {
        ObjectOptimisticLockingFailureException lastOptimisticException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return transferService.executeTransfer(accountId, request);

            } catch (ObjectOptimisticLockingFailureException e) {
                lastOptimisticException = e;
                sleepSilently(RETRY_WAIT_MS);

            } catch (DuplicateIdempotencyKeyException e) {
                return transferService.findByIdempotencyKey(e.getIdempotencyKey())
                    .orElseThrow(() -> new WalletException(
                        "Idempotency key conflict but no transaction found — this should not happen", e));
            }
        }

        throw new WalletException(
            "Transfer failed after " + MAX_RETRIES + " attempts due to concurrent modification. " +
            "This is extremely rare; the caller may retry.",
            lastOptimisticException
        );
    }

    private AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getExternalReference(),
            account.getCurrency(),
            account.getBalance(),
            account.getCreatedAt()
        );
    }

    private TransactionResponse toTransactionResponse(Transaction tx) {
        return new TransactionResponse(
            tx.getId(),
            tx.getAccountId(),
            tx.getAmount(),
            tx.getPreBalance(),
            tx.getPostBalance(),
            tx.getType(),
            tx.getDescription(),
            tx.getCreatedAt(),
            tx.getIdempotencyKey()
        );
    }

    private void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
