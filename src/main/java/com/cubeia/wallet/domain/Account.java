package com.cubeia.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String externalReference;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private long balance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Account() {
        // JPA
    }

    public Account(String id, String externalReference, String currency, long initialBalance) {
        this.id = id;
        this.externalReference = externalReference;
        this.currency = currency;
        this.balance = initialBalance;
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getExternalReference() { return externalReference; }
    public String getCurrency() { return currency; }
    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
