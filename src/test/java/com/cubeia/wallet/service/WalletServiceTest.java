package com.cubeia.wallet.service;

import com.cubeia.wallet.dto.AccountResponse;
import com.cubeia.wallet.dto.CreateAccountRequest;
import com.cubeia.wallet.exception.AccountNotFoundException;
import com.cubeia.wallet.exception.DuplicateAccountException;
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

        assertThat(created.id()).isNotNull();
        assertThat(created.balance()).isEqualTo(1000L);
        assertThat(created.currency()).isEqualTo("EUR");
    }

    @Test
    void createAccount_duplicateExternalReference_throws() {
        String ref = "player-" + UUID.randomUUID();
        walletService.createAccount(new CreateAccountRequest(ref, "EUR", 0L));

        assertThatThrownBy(() -> walletService.createAccount(
            new CreateAccountRequest(ref, "USD", 0L)))
            .isInstanceOf(DuplicateAccountException.class);
    }

    @Test
    void getBalance_unknownAccount_throws() {
        assertThatThrownBy(() -> walletService.getBalance("does-not-exist"))
            .isInstanceOf(AccountNotFoundException.class);
    }
}
