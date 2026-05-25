package com.cubeia.wallet.seed;

import com.cubeia.wallet.dto.CreateAccountRequest;
import com.cubeia.wallet.dto.TransferRequest;
import com.cubeia.wallet.domain.TransactionType;
import com.cubeia.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds demo accounts and transactions on startup.
 * Excluded from tests via @Profile("!test") to avoid polluting test data.
 */
@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final WalletService walletService;

    public DataSeeder(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding demo data...");

        String alice = seed("alice", "EUR", 100_000L);
        String bob = seed("bob", "USD", 50_000L);
        String charlie = seed("charlie", "EUR", 200_000L);
        String diana = seed("diana", "GBP", 75_000L);
        String eve = seed("eve", "USD", 30_000L);

        transfer(alice, 5000L, TransactionType.DEBIT, "Coffee shop");
        transfer(alice, 20000L, TransactionType.CREDIT, "Salary");
        transfer(bob, 1500L, TransactionType.DEBIT, "Grocery store");
        transfer(bob, 10000L, TransactionType.DEBIT, "Rent");
        transfer(charlie, 50000L, TransactionType.DEBIT, "Car repair");
        transfer(charlie, 80000L, TransactionType.CREDIT, "Freelance invoice");
        transfer(diana, 3000L, TransactionType.DEBIT, "Subscription");
        transfer(diana, 15000L, TransactionType.CREDIT, "Bonus");
        transfer(eve, 7500L, TransactionType.DEBIT, "Online purchase");
        transfer(eve, 25000L, TransactionType.CREDIT, "Tax refund");
        transfer(alice, 1200L, TransactionType.DEBIT, "Streaming service");
        transfer(bob, 5000L, TransactionType.CREDIT, "Side project");

        log.info("Demo data seeded: 5 accounts, 12 transactions");
    }

    private String seed(String name, String currency, long initialBalance) {
        try {
            var resp = walletService.createAccount(
                new CreateAccountRequest(name, currency, initialBalance));
            return resp.id();
        } catch (Exception e) {
            log.warn("Account '{}' already exists, skipping seed", name);
            return null;
        }
    }

    private void transfer(String accountId, long amount, TransactionType type, String description) {
        if (accountId == null) return;
        try {
            walletService.transfer(accountId, new TransferRequest(amount, type, description, null));
        } catch (Exception e) {
            log.warn("Seed transfer failed for account {}: {}", accountId, e.getMessage());
        }
    }
}
