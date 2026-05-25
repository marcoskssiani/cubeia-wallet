package com.cubeia.wallet.event;

import com.cubeia.wallet.domain.TransactionType;

import java.time.Instant;

public record TransactionCreatedEvent(
    String transactionId,
    String accountId,
    String externalReference,
    String currency,
    long amount,
    TransactionType transactionType,
    String description,
    long postBalance,
    Instant timestamp
) {}
