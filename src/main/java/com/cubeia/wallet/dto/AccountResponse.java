package com.cubeia.wallet.dto;

import java.time.Instant;

public record AccountResponse(
    String id,
    String externalReference,
    String currency,
    long balance,
    Instant createdAt
) {}
