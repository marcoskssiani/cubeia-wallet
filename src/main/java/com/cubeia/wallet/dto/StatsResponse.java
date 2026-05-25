package com.cubeia.wallet.dto;

public record StatsResponse(
    long accountCount,
    long totalBalance,
    long recentTransactionCount,
    int activeConnections
) {}
