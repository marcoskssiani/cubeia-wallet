package com.cubeia.wallet.exception;

public class InsufficientFundsException extends WalletException {

    private final long currentBalance;
    private final long requestedDebit;

    public InsufficientFundsException(long currentBalance, long requestedDebit) {
        super(String.format(
            "Insufficient funds: balance=%d cents, requested debit=%d cents",
            currentBalance, requestedDebit));
        this.currentBalance = currentBalance;
        this.requestedDebit = requestedDebit;
    }

    public long getCurrentBalance() { return currentBalance; }
    public long getRequestedDebit() { return requestedDebit; }
}
