package com.cubeia.wallet.service;

import com.cubeia.wallet.domain.TransactionType;
import com.cubeia.wallet.dto.AccountResponse;
import com.cubeia.wallet.dto.CreateAccountRequest;
import com.cubeia.wallet.dto.TransactionListResponse;
import com.cubeia.wallet.dto.TransactionResponse;
import com.cubeia.wallet.dto.TransferRequest;
import com.cubeia.wallet.exception.AccountNotFoundException;
import com.cubeia.wallet.exception.DuplicateAccountException;
import com.cubeia.wallet.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class WalletServiceTest {

    @Autowired
    WalletService walletService;

    @Test
    void createAccount_happyPath() {
        AccountResponse created = walletService.createAccount(
            new CreateAccountRequest("player-" + UUID.randomUUID(), "EUR", 1000L)
        );
        assertThat(created.balance()).isEqualTo(1000L);
    }

    @Test
    void createAccount_duplicate_throws() {
        String ref = "player-" + UUID.randomUUID();
        walletService.createAccount(new CreateAccountRequest(ref, "EUR", 0L));
        assertThatThrownBy(() -> walletService.createAccount(
            new CreateAccountRequest(ref, "USD", 0L)))
            .isInstanceOf(DuplicateAccountException.class);
    }

    @Test
    void transfer_credit_increasesBalance() {
        String id = walletService.createAccount(
            new CreateAccountRequest("p-" + UUID.randomUUID(), "EUR", 0L)).id();

        TransactionResponse tx = walletService.transfer(id,
            new TransferRequest(500L, TransactionType.CREDIT, "deposit"));

        assertThat(tx.amount()).isEqualTo(500L);
        assertThat(tx.preBalance()).isEqualTo(0L);
        assertThat(tx.postBalance()).isEqualTo(500L);
        assertThat(walletService.getBalance(id).balance()).isEqualTo(500L);
    }

    @Test
    void transfer_debit_decreasesBalance() {
        String id = walletService.createAccount(
            new CreateAccountRequest("p-" + UUID.randomUUID(), "EUR", 1000L)).id();

        TransactionResponse tx = walletService.transfer(id,
            new TransferRequest(300L, TransactionType.DEBIT, "withdraw"));

        assertThat(tx.amount()).isEqualTo(-300L);
        assertThat(tx.postBalance()).isEqualTo(700L);
    }

    @Test
    void transfer_debitExceedingBalance_throws() {
        String id = walletService.createAccount(
            new CreateAccountRequest("p-" + UUID.randomUUID(), "EUR", 100L)).id();

        assertThatThrownBy(() -> walletService.transfer(id,
            new TransferRequest(500L, TransactionType.DEBIT, "overdraft attempt")))
            .isInstanceOf(InsufficientFundsException.class);

        assertThat(walletService.getBalance(id).balance()).isEqualTo(100L);
    }

    @Test
    void transfer_unknownAccount_throws() {
        assertThatThrownBy(() -> walletService.transfer("no-such-id",
            new TransferRequest(100L, TransactionType.CREDIT, "x")))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void listTransactions_newestFirst() {
        String id = walletService.createAccount(
            new CreateAccountRequest("p-" + UUID.randomUUID(), "EUR", 0L)).id();
        walletService.transfer(id, new TransferRequest(100L, TransactionType.CREDIT, "first"));
        walletService.transfer(id, new TransferRequest(200L, TransactionType.CREDIT, "second"));

        TransactionListResponse list = walletService.listTransactions(id, 0, 20);

        assertThat(list.transactions()).hasSize(2);
        assertThat(list.transactions().get(0).description()).isEqualTo("second");
        assertThat(list.transactions().get(1).description()).isEqualTo("first");
    }
}
