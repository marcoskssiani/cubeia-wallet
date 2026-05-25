package com.cubeia.wallet.dto;

import java.util.List;

public record TransactionListResponse(
    String accountId,
    List<TransactionResponse> transactions,
    long totalElements,
    int page,
    int size
) {}
