package com.cubeia.wallet.dto;

import com.cubeia.wallet.domain.TransactionType;

import java.time.Instant;

public record TransactionResponse(
    String id,
    String accountId,
    long amount,
    long preBalance,
    long postBalance,
    TransactionType type,
    String description,
    Instant createdAt
) {}
