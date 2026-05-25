package com.cubeia.wallet.repository;

import com.cubeia.wallet.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByExternalReference(String externalReference);

    List<Account> findAllByOrderByCreatedAtDesc();
}
