package com.cubeia.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAccountRequest(

    @NotBlank(message = "externalReference is required")
    String externalReference,

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO 4217 code (e.g. EUR, USD)")
    String currency,

    @PositiveOrZero(message = "initialBalance must be >= 0")
    Long initialBalance
) {
    public long resolvedInitialBalance() {
        return initialBalance != null ? initialBalance : 0L;
    }
}
