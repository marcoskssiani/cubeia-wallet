package com.cubeia.wallet.dto;

public record BalanceResponse(
    String accountId,
    String currency,
    long balance
) {}
