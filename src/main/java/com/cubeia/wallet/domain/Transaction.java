package com.cubeia.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable ledger entry.
 * Design notes:
 * - amount is signed internally: positive = credit, negative = debit.
 * - preBalance / postBalance form the audit trail: at any moment,
 *   postBalance of transaction N must equal preBalance of transaction N+1.
 *
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_account_id", columnList = "accountId")
    }
)
public class Transaction {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, updatable = false)
    private String accountId;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Column(nullable = false, updatable = false)
    private long preBalance;

    @Column(nullable = false, updatable = false)
    private long postBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, updatable = false)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {
        // JPA
    }

    public Transaction(String id, String accountId, long amount, long preBalance,
                       long postBalance, TransactionType type, String description) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.preBalance = preBalance;
        this.postBalance = postBalance;
        this.type = type;
        this.description = description;
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public long getAmount() { return amount; }
    public long getPreBalance() { return preBalance; }
    public long getPostBalance() { return postBalance; }
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
