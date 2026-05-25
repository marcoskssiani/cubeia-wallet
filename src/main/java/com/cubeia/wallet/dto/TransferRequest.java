package com.cubeia.wallet.dto;

import com.cubeia.wallet.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    Long amount,

    @NotNull(message = "type is required (CREDIT or DEBIT)")
    TransactionType type,

    @NotBlank(message = "description is required")
    String description
) {}
