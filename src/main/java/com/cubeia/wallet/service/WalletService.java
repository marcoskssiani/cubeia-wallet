package com.cubeia.wallet.service;

import com.cubeia.wallet.domain.Account;
import com.cubeia.wallet.dto.AccountResponse;
import com.cubeia.wallet.dto.BalanceResponse;
import com.cubeia.wallet.dto.CreateAccountRequest;
import com.cubeia.wallet.exception.AccountNotFoundException;
import com.cubeia.wallet.exception.DuplicateAccountException;
import com.cubeia.wallet.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private final AccountRepository accountRepository;

    public WalletService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.findByExternalReference(request.externalReference()).isPresent()) {
            throw new DuplicateAccountException(request.externalReference());
        }
        Account account = new Account(
            UUID.randomUUID().toString(),
            request.externalReference(),
            request.currency(),
            request.resolvedInitialBalance()
        );
        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
        return new BalanceResponse(account.getId(), account.getCurrency(), account.getBalance());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAllAccounts() {
        return accountRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getExternalReference(),
            account.getCurrency(),
            account.getBalance(),
            account.getCreatedAt()
        );
    }
}
