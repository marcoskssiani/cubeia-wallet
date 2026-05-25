package com.cubeia.wallet.exception;

public class DuplicateAccountException extends WalletException {
    public DuplicateAccountException(String externalReference) {
        super("Account already exists with externalReference: " + externalReference);
    }
}
