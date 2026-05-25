package com.cubeia.wallet.repository;

import com.cubeia.wallet.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    long countByCreatedAtAfter(Instant since);

    List<Transaction> findByCreatedAtAfterOrderByCreatedAtAsc(Instant since);
}
