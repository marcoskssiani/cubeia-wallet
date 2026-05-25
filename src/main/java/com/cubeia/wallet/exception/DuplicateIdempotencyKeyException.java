package com.cubeia.wallet.exception;

public class DuplicateIdempotencyKeyException extends WalletException {

    private final String idempotencyKey;

    public DuplicateIdempotencyKeyException(String idempotencyKey, Throwable cause) {
        super("Duplicate idempotency key: " + idempotencyKey, cause);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
