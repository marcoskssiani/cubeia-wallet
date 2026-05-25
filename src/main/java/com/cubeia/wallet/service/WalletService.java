package com.cubeia.wallet.service;

import com.cubeia.wallet.domain.Account;
import com.cubeia.wallet.domain.Transaction;
import com.cubeia.wallet.domain.TransactionType;
import com.cubeia.wallet.dto.AccountResponse;
import com.cubeia.wallet.dto.BalanceResponse;
import com.cubeia.wallet.dto.CreateAccountRequest;
import com.cubeia.wallet.dto.TransactionListResponse;
import com.cubeia.wallet.dto.TransactionResponse;
import com.cubeia.wallet.dto.TransferRequest;
import com.cubeia.wallet.exception.AccountNotFoundException;
import com.cubeia.wallet.exception.DuplicateAccountException;
import com.cubeia.wallet.exception.InsufficientFundsException;
import com.cubeia.wallet.repository.AccountRepository;
import com.cubeia.wallet.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(AccountRepository accountRepository,
                         TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
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
        return toAccountResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
        return toAccountResponse(account);
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
            .map(this::toAccountResponse)
            .toList();
    }

    @Transactional
    public TransactionResponse transfer(String accountId, TransferRequest request) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        long signedAmount = request.type() == TransactionType.CREDIT
            ? request.amount()
            : -request.amount();

        long preBalance = account.getBalance();
        long postBalance = preBalance + signedAmount;

        if (postBalance < 0) {
            throw new InsufficientFundsException(preBalance, request.amount());
        }

        account.setBalance(postBalance);
        accountRepository.save(account);

        Transaction tx = new Transaction(
            UUID.randomUUID().toString(),
            accountId,
            signedAmount,
            preBalance,
            postBalance,
            request.type(),
            request.description()
        );
        transactionRepository.save(tx);

        return toTransactionResponse(tx);
    }

    @Transactional(readOnly = true)
    public TransactionListResponse listTransactions(String accountId, int page, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        Page<Transaction> txPage = transactionRepository
            .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(page, size));

        List<TransactionResponse> responses = txPage.getContent().stream()
            .map(this::toTransactionResponse)
            .toList();

        return new TransactionListResponse(accountId, responses, txPage.getTotalElements(), page, size);
    }

    private AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getExternalReference(),
            account.getCurrency(),
            account.getBalance(),
            account.getCreatedAt()
        );
    }

    private TransactionResponse toTransactionResponse(Transaction tx) {
        return new TransactionResponse(
            tx.getId(),
            tx.getAccountId(),
            tx.getAmount(),
            tx.getPreBalance(),
            tx.getPostBalance(),
            tx.getType(),
            tx.getDescription(),
            tx.getCreatedAt()
        );
    }
}
