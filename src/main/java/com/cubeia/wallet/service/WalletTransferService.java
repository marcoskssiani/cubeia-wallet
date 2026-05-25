package com.cubeia.wallet.service;

import com.cubeia.wallet.domain.Account;
import com.cubeia.wallet.domain.Transaction;
import com.cubeia.wallet.domain.TransactionType;
import com.cubeia.wallet.dto.TransactionResponse;
import com.cubeia.wallet.dto.TransferRequest;
import com.cubeia.wallet.event.TransactionCreatedEvent;
import com.cubeia.wallet.exception.AccountNotFoundException;
import com.cubeia.wallet.exception.DuplicateIdempotencyKeyException;
import com.cubeia.wallet.exception.InsufficientFundsException;
import com.cubeia.wallet.repository.AccountRepository;
import com.cubeia.wallet.repository.TransactionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletTransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WalletTransferService(AccountRepository accountRepository,
                                  TransactionRepository transactionRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TransactionResponse executeTransfer(String accountId, TransferRequest request) {
        if (request.idempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository
                .findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        long signedAmount = request.type() == TransactionType.CREDIT
            ? request.amount()
            : -request.amount();

        long preBalance = account.getBalance();
        long postBalance = preBalance + signedAmount;

        if (postBalance < 0) {
            throw new InsufficientFundsException(preBalance, request.amount());
        }

        account.setBalance(postBalance);
        accountRepository.saveAndFlush(account);

        Transaction tx = new Transaction(
            UUID.randomUUID().toString(),
            accountId,
            signedAmount,
            preBalance,
            postBalance,
            request.type(),
            request.description(),
            request.idempotencyKey()
        );

        try {
            transactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException e) {
            if (request.idempotencyKey() != null && isIdempotencyKeyConflict(e)) {
                throw new DuplicateIdempotencyKeyException(request.idempotencyKey(), e);
            }
            throw e;
        }

        eventPublisher.publishEvent(new TransactionCreatedEvent(
            tx.getId(),
            accountId,
            account.getExternalReference(),
            account.getCurrency(),
            signedAmount,
            request.type(),
            request.description(),
            postBalance,
            tx.getCreatedAt() != null ? tx.getCreatedAt() : Instant.now()
        ));

        return toResponse(tx);
    }

    @Transactional(readOnly = true)
    public Optional<TransactionResponse> findByIdempotencyKey(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
            .map(this::toResponse);
    }

    private boolean isIdempotencyKeyConflict(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        String message = cause != null ? cause.getMessage() : e.getMessage();
        return message != null && message.toLowerCase().contains("idempotency_key");
    }

    private TransactionResponse toResponse(Transaction tx) {
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
}
