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
 *
 * - amount is signed internally: positive = credit, negative = debit.
 * - preBalance / postBalance form the audit trail.
 * - idempotencyKey has a UNIQUE constraint; inserting the same key twice
 *   throws DataIntegrityViolationException, which the service converts into
 *   a typed DuplicateIdempotencyKeyException and recovers from gracefully.
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_account_id", columnList = "accountId"),
        @Index(name = "idx_transactions_idempotency_key", columnList = "idempotencyKey", unique = true)
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

    @Column(updatable = false, unique = true)
    private String idempotencyKey;

    protected Transaction() {
        // JPA
    }

    public Transaction(String id, String accountId, long amount, long preBalance,
                       long postBalance, TransactionType type, String description,
                       String idempotencyKey) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.preBalance = preBalance;
        this.postBalance = postBalance;
        this.type = type;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
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
    public String getIdempotencyKey() { return idempotencyKey; }

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
