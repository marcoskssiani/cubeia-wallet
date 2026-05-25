package com.cubeia.wallet.exception;

public class AccountNotFoundException extends WalletException {
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }
}
